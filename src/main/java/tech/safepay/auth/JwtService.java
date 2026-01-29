package tech.safepay.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Serviço responsável pela geração, validação e parse de tokens JWT (JSON Web Tokens).
 * <p>
 * Este serviço gerencia todo o ciclo de vida dos access tokens JWT usados para
 * autenticação de administradores. Utiliza a biblioteca JJWT para operações criptográficas
 * e assinatura HMAC-SHA256.
 * </p>
 * <p>
 * <b>Características dos tokens gerados:</b>
 * <ul>
 *   <li>Algoritmo: HS256 (HMAC com SHA-256)</li>
 *   <li>Stateless: não requerem consulta ao banco de dados para validação</li>
 *   <li>Autocontidos: todas as informações necessárias estão no próprio token</li>
 *   <li>Assinados: garantem integridade e autenticidade</li>
 *   <li>Curta duração: padrão de 15 minutos para minimizar riscos</li>
 * </ul>
 * </p>
 * <p>
 * <b>⚠️ SEGURANÇA IMPORTANTE:</b>
 * <ul>
 *   <li>A secret key DEVE ter no mínimo 256 bits (32 caracteres)</li>
 *   <li>NUNCA commite a secret key real no código-fonte</li>
 *   <li>Use variáveis de ambiente em produção</li>
 *   <li>Rotacione a secret key periodicamente</li>
 * </ul>
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AdminJwtFilter
 * @see AuthService
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    /**
     * Construtor que inicializa a chave secreta e tempo de expiração dos tokens.
     * <p>
     * A secret key é convertida de String para {@link SecretKey} usando HMAC-SHA256,
     * garantindo que tenha o comprimento e formato adequados para assinatura JWT.
     * </p>
     * <p>
     * <b>Configuração via application.properties:</b>
     * <pre>
     * jwt.secret=sua-chave-secreta-de-no-minimo-256-bits-aqui
     * jwt.access-token-expiration=900000
     * </pre>
     * </p>
     * <p>
     * <b>Boas práticas de produção:</b>
     * <pre>
     * # Usar variável de ambiente
     * jwt.secret=${JWT_SECRET}
     *
     * # Ou usar secret manager (AWS Secrets Manager, Azure Key Vault, etc)
     * </pre>
     * </p>
     *
     * @param secret string da chave secreta (mínimo 256 bits / 32 caracteres para HS256)
     * @param accessTokenExpiration tempo de validade do token em milissegundos (padrão: 900000ms = 15 min)
     *
     * @throws IllegalArgumentException se a secret key for muito curta (< 256 bits)
     */
    public JwtService(
            @Value("${jwt.secret:your-256-bit-secret-key}") String secret,
            @Value("${jwt.access-token-expiration:900000}") long accessTokenExpiration
    ) {
        // Converte a string da secret key para SecretKey usando UTF-8
        // Keys.hmacShaKeyFor garante que a chave tenha tamanho adequado para HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * Gera um novo access token JWT para o administrador especificado.
     * <p>
     * O token gerado é um JWT compacto (formato: header.payload.signature) contendo:
     * <ul>
     *   <li><b>subject:</b> UUID do admin (identificador único)</li>
     *   <li><b>type:</b> "ADMIN" (usado pelo filtro para validar tipo de token)</li>
     *   <li><b>email:</b> Email do admin (útil para logging e auditoria)</li>
     *   <li><b>issuedAt:</b> Timestamp de quando o token foi criado</li>
     *   <li><b>expiration:</b> Timestamp de quando o token expira</li>
     * </ul>
     * </p>
     * <p>
     * <b>Estrutura do token JWT gerado:</b>
     * <pre>
     * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.          ← Header (algoritmo + tipo)
     * eyJzdWIiOiI5ODc2LTEyMzQtNTY3OCIsInR5cGUiOiJ...  ← Payload (claims/dados)
     * SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c     ← Signature (assinatura)
     * </pre>
     * </p>
     * <p>
     * <b>Exemplo de payload decodificado:</b>
     * <pre>
     * {
     *   "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     *   "type": "ADMIN",
     *   "email": "admin@safepay.tech",
     *   "iat": 1706745600,
     *   "exp": 1706746500
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Por que curta duração?</b><br>
     * Access tokens têm vida curta (15 min) para limitar o impacto de vazamento.
     * Se um token for interceptado, ele só funciona por pouco tempo.
     * Para sessões longas, usa-se refresh tokens.
     * </p>
     *
     * @param admin entidade do administrador para o qual o token será gerado
     * @return string do token JWT assinado, pronto para ser enviado ao cliente
     *
     * @implNote O token é stateless e autocontido, não requerendo consulta ao banco
     *           para validação (exceto para verificar se não foi revogado globalmente)
     */
    public String generateAccessToken(AdminEntity admin) {
        return Jwts.builder()
                // Define o "subject" (sujeito) do token - identifica unicamente o admin
                .subject(admin.getId().toString())

                // Adiciona claim customizada indicando que este é um token de ADMIN
                // Usado pelo AdminJwtFilter para diferenciar de outros tipos de token
                .claim("type", "ADMIN")

                // Adiciona email como claim para facilitar logging sem parse de UUID
                .claim("email", admin.getEmail())

                // Define timestamp de criação (issued at) - útil para auditoria
                .issuedAt(new Date())

                // Define timestamp de expiração (agora + 15 minutos por padrão)
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))

                // Assina o token com a secret key usando HMAC-SHA256
                // A assinatura garante que o token não pode ser adulterado
                .signWith(secretKey)

                // Compacta tudo em string JWT final (header.payload.signature)
                .compact();
    }

    /**
     * Faz parse e validação de um token JWT, retornando suas claims (dados).
     * <p>
     * Este método realiza várias validações automáticas:
     * <ul>
     *   <li>Verifica a assinatura do token (previne adulteração)</li>
     *   <li>Valida que o token foi assinado com a secret key correta</li>
     *   <li>Verifica o formato do token</li>
     * </ul>
     * </p>
     * <p>
     * <b>⚠️ ATENÇÃO:</b> Este método NÃO verifica expiração automaticamente nas
     * versões mais recentes do JJWT. Use {@link #isTokenValid(String)} se precisar
     * validar expiração, ou verifique manualmente:
     * <pre>
     * Claims claims = parse(token);
     * if (claims.getExpiration().before(new Date())) {
     *     // Token expirado
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Exceções lançadas:</b>
     * <ul>
     *   <li><b>MalformedJwtException:</b> Token mal formatado</li>
     *   <li><b>SignatureException:</b> Assinatura inválida (token adulterado)</li>
     *   <li><b>ExpiredJwtException:</b> Token expirado (se validação estiver habilitada)</li>
     *   <li><b>UnsupportedJwtException:</b> Formato de token não suportado</li>
     * </ul>
     * </p>
     *
     * @param token string do token JWT a ser parseado
     * @return {@link Claims} objeto contendo todas as informações (claims) do token
     *
     * @throws io.jsonwebtoken.JwtException se o token for inválido, adulterado ou mal formatado
     *
     * @see #isTokenValid(String)
     */
    public Claims parse(String token) {
        return Jwts.parser()
                // Configura a secret key para verificação da assinatura
                .verifyWith(secretKey)

                // Constrói o parser com as configurações
                .build()

                // Faz parse do token e valida a assinatura
                .parseSignedClaims(token)

                // Retorna o payload (corpo) do token contendo as claims
                .getPayload();
    }

    /**
     * Valida se um token JWT é válido e ainda não expirou.
     * <p>
     * Este método é uma validação completa que verifica:
     * <ol>
     *   <li>Formato do token está correto</li>
     *   <li>Assinatura é válida (token não foi adulterado)</li>
     *   <li>Token não expirou (exp claim &gt; now)</li>
     * </ol>
     * </p>
     * <p>
     * <b>Uso típico:</b>
     * <pre>
     * if (jwtService.isTokenValid(token)) {
     *     // Processa requisição autenticada
     * } else {
     *     // Retorna 401 Unauthorized
     * }
     * </pre>
     * </p>
     * <p>
     * <b>Diferença entre parse() e isTokenValid():</b>
     * <ul>
     *   <li><b>parse():</b> Lança exceção se inválido, retorna claims se válido</li>
     *   <li><b>isTokenValid():</b> Retorna boolean, nunca lança exceção</li>
     * </ul>
     * </p>
     * <p>
     * <b>Performance:</b> Este método faz parse completo do token (operação criptográfica).
     * Para alta performance em produção, considere cachear resultados se o mesmo token
     * for validado múltiplas vezes em curto período.
     * </p>
     *
     * @param token string do token JWT a ser validado
     * @return {@code true} se o token for válido e não expirado, {@code false} caso contrário
     *
     * @implNote Usa try-catch para capturar qualquer exceção de validação e retornar
     *           false de forma segura, evitando exposição de detalhes de erro
     */
    public boolean isTokenValid(String token) {
        try {
            // Tenta fazer parse do token (valida assinatura e formato)
            Claims claims = parse(token);

            // Verifica se o token ainda não expirou
            // getExpiration() retorna a data/hora de expiração do token
            // after(new Date()) verifica se exp > agora
            return claims.getExpiration().after(new Date());

        } catch (Exception e) {
            // Qualquer exceção (assinatura inválida, formato incorreto, etc) = token inválido
            return false;
        }
    }
}