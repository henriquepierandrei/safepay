package tech.safepay.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST responsável pelos endpoints de autenticação administrativa.
 * <p>
 * Expõe APIs públicas para login, renovação de tokens (refresh) e logout de administradores.
 * Todos os endpoints deste controller são públicos e não requerem autenticação prévia.
 * </p>
 * <p>
 * <b>Base Path:</b> {@code /admin/auth}
 * </p>
 * <p>
 * <b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>POST /admin/auth/login - Autenticação com email e senha</li>
 *   <li>POST /admin/auth/refresh - Renovação de access token usando refresh token</li>
 *   <li>POST /admin/auth/logout - Invalidação de todos os tokens do admin</li>
 * </ul>
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AuthService
 * @see AdminJwtFilter
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Construtor com injeção de dependência do serviço de autenticação.
     *
     * @param authService serviço que contém a lógica de negócio para autenticação
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint de autenticação (login) de administradores.
     * <p>
     * Valida as credenciais fornecidas (email e senha) e, se válidas, retorna
     * um access token JWT e um refresh token para o cliente.
     * </p>
     * <p>
     * <b>Fluxo de autenticação:</b>
     * <ol>
     *   <li>Valida se existe um admin com o email fornecido</li>
     *   <li>Verifica se a senha fornecida corresponde ao hash armazenado (BCrypt)</li>
     *   <li>Atualiza timestamp de último login do admin</li>
     *   <li>Gera novo access token JWT (válido por 15 minutos)</li>
     *   <li>Cria e persiste novo refresh token (válido por 7 dias)</li>
     *   <li>Retorna ambos os tokens ao cliente</li>
     * </ol>
     * </p>
     * <p>
     * <b>Request Body:</b>
     * <pre>
     * {
     *   "email": "admin@safepay.tech",
     *   "password": "Admin@123"
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Response Body (200 OK):</b>
     * <pre>
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     *   "email": "admin@safepay.tech"
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Possíveis erros:</b>
     * <ul>
     *   <li>401 UNAUTHORIZED - Credenciais inválidas (email não existe ou senha incorreta)</li>
     *   <li>400 BAD REQUEST - Dados malformados ou ausentes no request body</li>
     * </ul>
     * </p>
     *
     * @param request objeto contendo email e senha do administrador
     * @return {@link ResponseEntity} com status 200 e tokens de autenticação no corpo
     *
     * @throws UnauthorizedException se as credenciais forem inválidas
     *
     * @see AuthService#login(AuthService.LoginRequest)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResponse> login(@RequestBody AuthService.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Endpoint para renovação de access token usando refresh token.
     * <p>
     * Implementa o padrão de <b>Refresh Token Rotation</b>: quando um refresh token
     * é usado para gerar novos tokens, ele é imediatamente revogado e um novo
     * refresh token é emitido, aumentando a segurança.
     * </p>
     * <p>
     * <b>Fluxo de renovação:</b>
     * <ol>
     *   <li>Valida o refresh token recebido (existe, não revogado, não expirado)</li>
     *   <li>Revoga imediatamente o refresh token usado (rotation)</li>
     *   <li>Gera novo access token JWT para o mesmo admin</li>
     *   <li>Cria e persiste novo refresh token</li>
     *   <li>Retorna os novos tokens ao cliente</li>
     * </ol>
     * </p>
     * <p>
     * <b>Request Body:</b>
     * <pre>
     * {
     *   "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Response Body (200 OK):</b>
     * <pre>
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "z9y8x7w6-v5u4-3210-zyxw-vu9876543210",
     *   "email": "admin@safepay.tech"
     * }
     * </pre>
     * </p>
     * <p>
     * <b>⚠️ IMPORTANTE - Token Rotation:</b><br>
     * O refresh token antigo NÃO pode mais ser reutilizado após esta chamada.
     * O cliente DEVE armazenar o novo refresh token retornado na resposta.
     * </p>
     * <p>
     * <b>Possíveis erros:</b>
     * <ul>
     *   <li>401 UNAUTHORIZED - Refresh token inválido, expirado ou já revogado</li>
     *   <li>400 BAD REQUEST - Refresh token ausente ou malformado</li>
     * </ul>
     * </p>
     *
     * @param request objeto contendo o refresh token a ser validado e renovado
     * @return {@link ResponseEntity} com status 200 e novos tokens de autenticação
     *
     * @throws UnauthorizedException se o refresh token for inválido ou expirado
     *
     * @see AuthService#refresh(String)
     * @see AdminRefreshTokenService#validate(String)
     * @see AdminRefreshTokenService#revoke(AdminRefreshToken)
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthService.AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Endpoint de logout que invalida todos os refresh tokens do administrador.
     * <p>
     * Realiza um "logout global", encerrando todas as sessões ativas do admin
     * em todos os dispositivos e navegadores simultaneamente.
     * </p>
     * <p>
     * <b>Fluxo de logout:</b>
     * <ol>
     *   <li>Valida o refresh token recebido</li>
     *   <li>Identifica o admin proprietário do token</li>
     *   <li>Revoga TODOS os refresh tokens ativos deste admin no banco de dados</li>
     *   <li>Retorna resposta vazia com status 204 (No Content)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Request Body:</b>
     * <pre>
     * {
     *   "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Response:</b> Status 204 NO CONTENT (sem corpo de resposta)
     * </p>
     * <p>
     * <b>Efeito no cliente:</b> Após o logout bem-sucedido, o cliente deve:
     * <ul>
     *   <li>Remover o access token armazenado (localStorage, sessionStorage, cookies)</li>
     *   <li>Remover o refresh token armazenado</li>
     *   <li>Redirecionar para tela de login</li>
     *   <li>Limpar qualquer estado de autenticação na aplicação</li>
     * </ul>
     * </p>
     * <p>
     * <b>⚠️ ATENÇÃO - Logout Global:</b><br>
     * Este endpoint invalida TODAS as sessões do admin, não apenas a atual.
     * Se o admin estiver logado em múltiplos dispositivos, todos serão desconectados.
     * </p>
     * <p>
     * <b>Possíveis erros:</b>
     * <ul>
     *   <li>401 UNAUTHORIZED - Refresh token inválido ou expirado</li>
     *   <li>400 BAD REQUEST - Refresh token ausente</li>
     * </ul>
     * </p>
     *
     * @param request objeto contendo o refresh token do admin que está fazendo logout
     * @return {@link ResponseEntity} com status 204 NO CONTENT (sem corpo)
     *
     * @throws UnauthorizedException se o refresh token for inválido
     *
     * @see AuthService#logout(String)
     * @see AdminRefreshTokenService#revokeAllByAdmin(java.util.UUID)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Record que representa a requisição de renovação de token.
     * <p>
     * Usado como estrutura de dados imutável para desserialização automática
     * do JSON recebido nos endpoints de refresh e logout.
     * </p>
     * <p>
     * <b>Exemplo de JSON aceito:</b>
     * <pre>
     * {
     *   "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     * }
     * </pre>
     * </p>
     *
     * @param refreshToken string do refresh token enviada pelo cliente
     */
    public record RefreshTokenRequest(String refreshToken) {}
}