package tech.safepay.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Serviço de autenticação responsável pela lógica de negócio de login, refresh e logout.
 * <p>
 * Este serviço orquestra o fluxo completo de autenticação de administradores,
 * coordenando validação de credenciais, geração de tokens JWT, criação de refresh tokens
 * e gerenciamento de sessões.
 * </p>
 * <p>
 * <b>Funcionalidades principais:</b>
 * <ul>
 *   <li>Autenticação de administradores via email/senha</li>
 *   <li>Renovação de tokens usando refresh token rotation</li>
 *   <li>Logout com invalidação de todas as sessões</li>
 *   <li>Registro de último acesso para auditoria</li>
 * </ul>
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AdminEntity
 * @see JwtService
 * @see AdminRefreshTokenService
 */
@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AdminRefreshTokenService refreshService;

    /**
     * Record imutável representando a resposta de autenticação retornada ao cliente.
     * <p>
     * Contém todos os dados necessários para que o cliente possa armazenar e utilizar
     * os tokens de autenticação.
     * </p>
     *
     * @param token access token JWT (curta duração, ~15 minutos)
     * @param refreshToken token para renovação de access token (longa duração, ~7 dias)
     * @param email email do administrador autenticado (útil para exibição no frontend)
     */
    public record AuthResponse(String token, String refreshToken, String email) {}

    /**
     * Record imutável representando a requisição de login.
     * <p>
     * Contém as credenciais fornecidas pelo cliente para autenticação.
     * </p>
     *
     * @param email endereço de email do administrador
     * @param password senha em texto plano (será comparada com hash BCrypt armazenado)
     */
    public record LoginRequest(String email, String password) {}

    /**
     * Construtor com injeção de dependências necessárias para autenticação.
     *
     * @param adminRepository repositório para acesso aos dados de administradores
     * @param encoder encoder BCrypt para validação de senhas criptografadas
     * @param jwtService serviço para geração e validação de tokens JWT
     * @param refreshService serviço para gerenciamento de refresh tokens
     */
    public AuthService(
            AdminRepository adminRepository,
            PasswordEncoder encoder,
            JwtService jwtService,
            AdminRefreshTokenService refreshService
    ) {
        this.adminRepository = adminRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.refreshService = refreshService;
    }

    /**
     * Autentica um administrador usando email e senha.
     * <p>
     * <b>Fluxo de execução:</b>
     * <ol>
     *   <li>Busca admin no banco de dados pelo email fornecido</li>
     *   <li>Valida se a senha fornecida corresponde ao hash armazenado (BCrypt)</li>
     *   <li>Atualiza o timestamp de último login do admin</li>
     *   <li>Gera novo access token JWT contendo ID, tipo e email do admin</li>
     *   <li>Cria e persiste novo refresh token no banco de dados</li>
     *   <li>Retorna ambos os tokens para o cliente</li>
     * </ol>
     * </p>
     * <p>
     * <b>Segurança - Mensagens genéricas de erro:</b><br>
     * Tanto para email inexistente quanto para senha incorreta, a mensagem retornada
     * é genérica ("Invalid credentials") para prevenir enumeração de usuários.
     * Isso impede que atacantes descubram quais emails estão cadastrados no sistema.
     * </p>
     * <p>
     * <b>Auditoria:</b> O campo {@code lastLoginAt} é atualizado para permitir:
     * <ul>
     *   <li>Rastreamento de atividade de admins</li>
     *   <li>Detecção de contas inativas</li>
     *   <li>Identificação de acessos suspeitos (logins em horários incomuns)</li>
     * </ul>
     * </p>
     *
     * @param request objeto contendo email e senha fornecidos pelo cliente
     * @return {@link AuthResponse} com access token, refresh token e email do admin
     *
     * @throws UnauthorizedException se o email não existir ou a senha estiver incorreta
     *
     * @implNote Este método é transacional para garantir que a atualização do
     *           lastLoginAt e a criação do refresh token ocorram atomicamente
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Busca admin por email ou lança exceção se não encontrado
        AdminEntity admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        // Valida senha usando BCrypt (compara hash armazenado com senha fornecida)
        if (!encoder.matches(request.password(), admin.getHashPassword())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        // Registra timestamp do login atual para auditoria
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        // Gera e retorna tokens de autenticação
        return issueTokens(admin);
    }

    /**
     * Renova o access token usando um refresh token válido.
     * <p>
     * Implementa o padrão de <b>Refresh Token Rotation</b> para máxima segurança:
     * cada refresh token só pode ser usado uma única vez. Após o uso, ele é
     * imediatamente revogado e um novo é emitido.
     * </p>
     * <p>
     * <b>Fluxo de renovação:</b>
     * <ol>
     *   <li>Valida o refresh token (existe, não revogado, não expirado)</li>
     *   <li>Revoga IMEDIATAMENTE o refresh token usado (rotation)</li>
     *   <li>Gera novo access token JWT para o mesmo admin</li>
     *   <li>Cria e persiste novo refresh token</li>
     *   <li>Retorna os novos tokens ao cliente</li>
     * </ol>
     * </p>
     * <p>
     * <b>Por que Token Rotation aumenta a segurança?</b>
     * <ul>
     *   <li>Reduz janela de ataque: tokens comprometidos têm vida útil limitada</li>
     *   <li>Detecta reutilização: se um token revogado for usado novamente, indica possível ataque</li>
     *   <li>Limita danos: mesmo se interceptado, o token só funciona uma vez</li>
     * </ul>
     * </p>
     * <p>
     * <b>⚠️ IMPORTANTE PARA O CLIENTE:</b><br>
     * O cliente DEVE substituir o refresh token antigo pelo novo retornado nesta chamada.
     * Tentar reutilizar o refresh token antigo resultará em erro 401 UNAUTHORIZED.
     * </p>
     *
     * @param refreshToken string do refresh token a ser validado e renovado
     * @return {@link AuthResponse} com novo access token, novo refresh token e email
     *
     * @throws UnauthorizedException se o refresh token for inválido, expirado ou revogado
     *
     * @implNote A revogação acontece ANTES da emissão dos novos tokens para garantir
     *           que mesmo em caso de falha na geração, o token antigo não seja reutilizável
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        // Valida o refresh token ou lança exceção se inválido/expirado
        AdminRefreshToken stored = refreshService.validate(refreshToken);

        // 🔥 ROTATION: Revoga imediatamente o token usado (padrão de segurança)
        // Após esta linha, este refresh token NUNCA mais poderá ser usado
        refreshService.revoke(stored);

        // Gera e retorna novos tokens para o mesmo admin
        return issueTokens(stored.getAdmin());
    }

    /**
     * Realiza logout invalidando todos os refresh tokens do administrador.
     * <p>
     * Este é um <b>logout global</b> que encerra todas as sessões ativas do admin
     * em todos os dispositivos e navegadores simultaneamente.
     * </p>
     * <p>
     * <b>Fluxo de logout:</b>
     * <ol>
     *   <li>Valida o refresh token fornecido</li>
     *   <li>Identifica o admin proprietário do token</li>
     *   <li>Revoga TODOS os refresh tokens deste admin (em lote, via query SQL)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Efeito prático:</b>
     * <ul>
     *   <li>Admin logado no computador → deslogado</li>
     *   <li>Admin logado no celular → deslogado</li>
     *   <li>Admin logado em múltiplas abas → todas deslogadas</li>
     * </ul>
     * </p>
     * <p>
     * <b>Observação sobre Access Tokens:</b><br>
     * Os access tokens JWT continuam válidos até sua expiração natural (~15 minutos),
     * pois são stateless (não há como revogá-los sem manter uma blacklist).
     * Esta é uma característica do design JWT e aceita como trade-off de performance.
     * Para sessões críticas, considere tempos de expiração mais curtos.
     * </p>
     * <p>
     * <b>Casos de uso comuns:</b>
     * <ul>
     *   <li>Usuário clica em "Sair" no sistema</li>
     *   <li>Admin troca de senha (boa prática: invalidar sessões antigas)</li>
     *   <li>Suspeita de conta comprometida</li>
     *   <li>Administrador quer deslogar de todos os dispositivos remotamente</li>
     * </ul>
     * </p>
     *
     * @param refreshToken string do refresh token do admin que está fazendo logout
     *
     * @throws UnauthorizedException se o refresh token for inválido ou expirado
     *
     * @implNote Usa operação em lote (bulk update) para eficiência, evitando
     *           carregar todos os tokens na memória antes de revogá-los
     *
     * @see AdminRefreshTokenService#revokeAllByAdmin(java.util.UUID)
     */
    @Transactional
    public void logout(String refreshToken) {
        // Valida o refresh token e obtém o admin associado
        AdminRefreshToken stored = refreshService.validate(refreshToken);

        // Revoga TODOS os refresh tokens deste admin (logout global)
        refreshService.revokeAllByAdmin(stored.getAdmin().getId());
    }

    /**
     * Método privado utilitário para geração de ambos os tokens (access e refresh).
     * <p>
     * Centraliza a lógica de emissão de tokens para evitar duplicação de código
     * entre os métodos {@link #login(LoginRequest)} e {@link #refresh(String)}.
     * </p>
     * <p>
     * <b>Tokens gerados:</b>
     * <ul>
     *   <li><b>Access Token (JWT):</b> Contém claims: adminId (subject), type=ADMIN, email.
     *       Válido por tempo curto (padrão: 15 minutos)</li>
     *   <li><b>Refresh Token:</b> UUID aleatório armazenado no banco.
     *       Válido por tempo longo (padrão: 7 dias)</li>
     * </ul>
     * </p>
     *
     * @param admin entidade do administrador para o qual os tokens serão gerados
     * @return {@link AuthResponse} contendo access token, refresh token e email
     *
     * @implNote O refresh token é persistido no banco de dados antes de retornar,
     *           garantindo que ele já está disponível para futuras validações
     */
    private AuthResponse issueTokens(AdminEntity admin) {
        // Gera access token JWT (stateless, autocontido)
        String access = jwtService.generateAccessToken(admin);

        // Cria e persiste refresh token no banco (stateful)
        String refresh = refreshService.create(admin).getToken();

        // Retorna resposta completa com ambos os tokens
        return new AuthResponse(access, refresh, admin.getEmail());
    }
}