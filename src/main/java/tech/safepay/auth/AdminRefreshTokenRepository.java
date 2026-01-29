package tech.safepay.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para gerenciamento de refresh tokens de administradores.
 * <p>
 * Fornece operações CRUD básicas herdadas de {@link JpaRepository} e queries
 * customizadas para validação, revogação e limpeza de tokens.
 * </p>
 * <p>
 * Este repositório é essencial para o mecanismo de autenticação JWT com refresh tokens,
 * implementando o padrão de Token Rotation para maior segurança.
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AdminRefreshToken
 * @see AdminRefreshTokenService
 */
@Repository
public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {

    /**
     * Busca um refresh token válido (não revogado) pela string do token.
     * <p>
     * Este método é utilizado durante o processo de refresh de access tokens.
     * Retorna o token apenas se ele existir E não estiver revogado.
     * </p>
     * <p>
     * <b>Query derivada automaticamente pelo Spring Data JPA:</b><br>
     * {@code SELECT * FROM admin_refresh_tokens WHERE token = ? AND revoked = false}
     * </p>
     *
     * @param token string do refresh token enviada pelo cliente
     * @return {@link Optional} contendo o token se encontrado e válido, ou vazio caso contrário
     *
     * @implNote Este método NÃO verifica expiração - apenas se o token não foi revogado.
     *           A validação de expiração deve ser feita em {@link AdminRefreshTokenService#validate(String)}
     */
    Optional<AdminRefreshToken> findByTokenAndRevokedFalse(String token);

    /**
     * Revoga todos os refresh tokens ativos de um administrador específico.
     * <p>
     * Marca todos os tokens não revogados do admin como {@code revoked = true},
     * invalidando todas as sessões ativas em todos os dispositivos.
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Logout global - admin quer encerrar todas as sessões</li>
     *   <li>Reset de senha - invalidar todos os tokens existentes</li>
     *   <li>Suspeita de comprometimento - revogar acesso imediato</li>
     *   <li>Desativação de conta admin</li>
     * </ul>
     * </p>
     * <p>
     * <b>Importante:</b> Este método requer anotação {@code @Transactional} no serviço
     * que o chama, pois é uma operação de modificação em lote.
     * </p>
     *
     * @param adminId UUID do administrador cujos tokens devem ser revogados
     *
     * @implNote Usa JPQL para atualização em lote, mais eficiente que carregar
     *           cada token e atualizá-lo individualmente
     */
    @Modifying
    @Query("UPDATE AdminRefreshToken t SET t.revoked = true WHERE t.admin.id = :adminId AND t.revoked = false")
    void revokeAllByAdminId(UUID adminId);

    /**
     * Remove fisicamente tokens expirados e revogados do banco de dados.
     * <p>
     * Realiza limpeza permanente (DELETE) de tokens que:
     * <ul>
     *   <li>Já expiraram (expiresAt &lt; now), OU</li>
     *   <li>Foram revogados (revoked = true)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Propósito:</b>
     * <ul>
     *   <li>Reduzir tamanho da tabela no banco de dados</li>
     *   <li>Melhorar performance de queries</li>
     *   <li>Conformidade com LGPD/GDPR - não manter dados desnecessários</li>
     *   <li>Prevenir acúmulo infinito de registros históricos</li>
     * </ul>
     * </p>
     * <p>
     * <b>Uso recomendado:</b> Executar periodicamente via job agendado
     * (ex: diariamente às 3h da manhã usando {@code @Scheduled}).
     * </p>
     * <p>
     * <b>Atenção:</b> Esta é uma operação destrutiva e irreversível. Certifique-se
     * de que não há necessidade de auditoria ou histórico desses tokens antes de deletá-los.
     * </p>
     *
     * @param now timestamp atual usado como referência para determinar tokens expirados
     *
     * @implNote A operação DELETE é executada diretamente no banco de dados,
     *           sem carregar entidades na memória, tornando-a muito eficiente
     *           mesmo para grandes volumes de dados
     *
     * @see AdminRefreshTokenService#cleanupExpiredTokens()
     */
    @Modifying
    @Query("DELETE FROM AdminRefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    void cleanupExpiredTokens(LocalDateTime now);
}