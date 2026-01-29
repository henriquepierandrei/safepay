package tech.safepay.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento do ciclo de vida dos refresh tokens de administradores.
 * <p>
 * Este serviço orquestra todas as operações relacionadas a refresh tokens, incluindo:
 * criação, validação, revogação e limpeza automática de tokens expirados.
 * </p>
 * <p>
 * Implementa o padrão de <b>Refresh Token Rotation</b>: cada vez que um refresh token
 * é utilizado, ele é revogado e um novo é emitido, aumentando significativamente a segurança.
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AdminRefreshToken
 * @see AdminRefreshTokenRepository
 */
@Service
public class AdminRefreshTokenService {

    private final AdminRefreshTokenRepository repository;
    private final long refreshTokenExpiration;

    /**
     * Construtor com injeção de dependências e configuração de expiração.
     * <p>
     * O tempo de expiração é configurável via propriedade {@code jwt.refresh-token-expiration}
     * no arquivo application.properties. Se não configurado, usa 7 dias como padrão.
     * </p>
     *
     * @param repository repositório JPA para persistência de refresh tokens
     * @param refreshTokenExpiration tempo de expiração em milissegundos (padrão: 604800000ms = 7 dias)
     */
    public AdminRefreshTokenService(
            AdminRefreshTokenRepository repository,
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration
    ) {
        this.repository = repository;
        this.refreshTokenExpiration = refreshTokenExpiration / 1000; // Converte de milissegundos para segundos
    }

    /**
     * Cria e persiste um novo refresh token para o administrador especificado.
     * <p>
     * O token gerado possui as seguintes características:
     * <ul>
     *   <li>String aleatória baseada em UUID (imprevisível e único)</li>
     *   <li>Associado ao admin fornecido</li>
     *   <li>Data de expiração configurável (padrão: 7 dias a partir da criação)</li>
     *   <li>Status inicial: não revogado</li>
     * </ul>
     * </p>
     * <p>
     * <b>Quando usar:</b>
     * <ul>
     *   <li>Após login bem-sucedido</li>
     *   <li>Após renovação de token (rotation)</li>
     * </ul>
     * </p>
     *
     * @param admin entidade do administrador que receberá o refresh token
     * @return refresh token criado e persistido no banco de dados
     *
     * @implNote Este método é transacional para garantir que o token seja
     *           persistido atomicamente com todas as suas propriedades
     */
    @Transactional
    public AdminRefreshToken create(AdminEntity admin) {
        AdminRefreshToken refresh = new AdminRefreshToken();
        refresh.setToken(UUID.randomUUID().toString());
        refresh.setAdmin(admin);
        refresh.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration));
        refresh.setRevoked(false);

        return repository.save(refresh);
    }

    /**
     * Valida um refresh token verificando sua existência, revogação e expiração.
     * <p>
     * <b>Validações realizadas (em ordem):</b>
     * <ol>
     *   <li>Verifica se o token existe no banco de dados</li>
     *   <li>Verifica se o token NÃO foi revogado</li>
     *   <li>Verifica se o token NÃO expirou</li>
     * </ol>
     * </p>
     * <p>
     * <b>Importante:</b> Este método apenas valida, mas NÃO revoga o token.
     * A revogação deve ser feita explicitamente após o uso bem-sucedido
     * para implementar o padrão de Token Rotation.
     * </p>
     *
     * @param token string do refresh token a ser validado
     * @return refresh token válido encontrado no banco de dados
     *
     * @throws UnauthorizedException se o token não existir, estiver revogado ou expirado
     *
     * @implNote Usa transação read-only para otimizar performance, já que
     *           não há modificação de dados nesta operação
     */
    @Transactional(readOnly = true)
    public AdminRefreshToken validate(String token) {
        AdminRefreshToken refresh = repository
                .findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (refresh.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expirado");
        }

        return refresh;
    }

    /**
     * Revoga um refresh token específico, tornando-o inválido para uso futuro.
     * <p>
     * Marca o token como {@code revoked = true} e persiste a alteração no banco.
     * Após a revogação, o token não poderá mais ser usado para gerar novos access tokens.
     * </p>
     * <p>
     * <b>Casos de uso típicos:</b>
     * <ul>
     *   <li>Após uso bem-sucedido do token (Token Rotation)</li>
     *   <li>Logout de uma sessão específica</li>
     *   <li>Detecção de uso suspeito do token</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota:</b> A revogação é irreversível. Uma vez revogado, o token
     * permanece assim permanentemente até ser removido pela limpeza automática.
     * </p>
     *
     * @param token entidade do refresh token a ser revogado
     *
     * @implNote Prefere-se receber a entidade ao invés da string do token
     *           para evitar consulta dupla ao banco de dados
     */
    @Transactional
    public void revoke(AdminRefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    /**
     * Revoga todos os refresh tokens ativos de um administrador específico.
     * <p>
     * Invalida simultaneamente todas as sessões ativas do admin em todos os
     * dispositivos e navegadores, forçando nova autenticação em todos os locais.
     * </p>
     * <p>
     * <b>Cenários de uso:</b>
     * <ul>
     *   <li><b>Logout global:</b> Admin quer encerrar todas as sessões abertas</li>
     *   <li><b>Mudança de senha:</b> Invalidar tokens antigos por segurança</li>
     *   <li><b>Suspeita de comprometimento:</b> Revogar acesso imediato em todos os dispositivos</li>
     *   <li><b>Desativação de conta:</b> Garantir que não há sessões ativas restantes</li>
     * </ul>
     * </p>
     * <p>
     * <b>Implementação:</b> Utiliza query customizada em lote para eficiência,
     * evitando carregar todos os tokens na memória.
     * </p>
     *
     * @param adminId UUID do administrador cujos tokens serão revogados
     *
     * @see AdminRefreshTokenRepository#revokeAllByAdminId(UUID)
     */
    @Transactional
    public void revokeAllByAdmin(UUID adminId) {
        repository.revokeAllByAdminId(adminId);
    }

    /**
     * Executa limpeza automática de refresh tokens expirados e revogados.
     * <p>
     * Remove permanentemente do banco de dados todos os tokens que:
     * <ul>
     *   <li>Já passaram da data de expiração, OU</li>
     *   <li>Foram marcados como revogados</li>
     * </ul>
     * </p>
     * <p>
     * <b>Agendamento:</b> Executado automaticamente todos os dias às 3h da manhã
     * via expressão cron {@code "0 0 3 * * ?"}.
     * </p>
     * <p>
     * <b>Benefícios:</b>
     * <ul>
     *   <li>Mantém a tabela de tokens enxuta e performática</li>
     *   <li>Reduz uso de espaço em disco</li>
     *   <li>Melhora velocidade de queries ao longo do tempo</li>
     *   <li>Conformidade com LGPD - não mantém dados desnecessários</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota:</b> Esta operação é destrutiva e irreversível. Tokens removidos
     * não podem ser recuperados. Considere implementar auditoria se histórico
     * completo for necessário para conformidade.
     * </p>
     *
     * @implNote Requer que {@code @EnableScheduling} esteja ativo na aplicação
     *           (configurado em {@code application.properties} com
     *           {@code spring.task.scheduling.enabled=true})
     *
     * @see AdminRefreshTokenRepository#cleanupExpiredTokens(LocalDateTime)
     */
    @Scheduled(cron = "0 0 3 * * ?") // Executa diariamente às 3h da manhã
    @Transactional
    public void cleanupExpiredTokens() {
        repository.cleanupExpiredTokens(LocalDateTime.now());
    }
}