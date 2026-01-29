package tech.safepay.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA que representa tokens de renovação (refresh tokens) para administradores.
 * <p>
 * Refresh tokens são utilizados para obter novos access tokens sem necessidade de
 * reautenticação. Possuem validade maior que access tokens e podem ser revogados
 * individualmente para segurança adicional.
 * </p>
 * <p>
 * Implementa o padrão de <b>Token Rotation</b>: quando um refresh token é usado,
 * ele é revogado e um novo é emitido, aumentando a segurança do sistema.
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 */
@Entity
@Table(name = "admin_refresh_tokens")
public class AdminRefreshToken {

    /**
     * Identificador único do refresh token no banco de dados.
     * Gerado automaticamente como UUID para garantir unicidade global.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * String do token propriamente dito, usado pelo cliente nas requisições.
     * <p>
     * Características:
     * <ul>
     *   <li>Único no banco de dados (constraint de unicidade)</li>
     *   <li>Obrigatório (não pode ser nulo)</li>
     *   <li>Comprimento máximo de 500 caracteres</li>
     *   <li>Gerado como UUID aleatório para imprevisibilidade</li>
     * </ul>
     * </p>
     */
    @Column(unique = true, nullable = false, length = 500)
    private String token;

    /**
     * Relacionamento Many-to-One com a entidade AdminEntity.
     * <p>
     * Um admin pode ter múltiplos refresh tokens ativos (múltiplos dispositivos/sessões),
     * mas cada refresh token pertence a apenas um admin.
     * </p>
     * <p>
     * Configurações:
     * <ul>
     *   <li><b>optional = false:</b> Todo refresh token DEVE ter um admin associado</li>
     *   <li><b>fetch = LAZY:</b> Admin só é carregado quando explicitamente acessado,
     *       melhorando performance em queries que não precisam dessa informação</li>
     *   <li><b>@JoinColumn:</b> Cria coluna "admin_id" na tabela para chave estrangeira</li>
     * </ul>
     * </p>
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminEntity admin;

    /**
     * Data e hora de expiração do refresh token.
     * <p>
     * Após esta data, o token não pode mais ser usado para gerar novos access tokens,
     * mesmo que não tenha sido revogado. Tipicamente configurado para 7 dias após criação.
     * </p>
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indica se o token foi revogado (invalidado) antes da expiração natural.
     * <p>
     * Tokens são revogados quando:
     * <ul>
     *   <li>Utilizados para gerar novo access token (token rotation)</li>
     *   <li>Admin faz logout</li>
     *   <li>Admin solicita invalidação de todos os tokens</li>
     *   <li>Detectada atividade suspeita</li>
     * </ul>
     * </p>
     * <p>
     * Valor padrão é {@code false}. Uma vez revogado, não pode ser revertido.
     * </p>
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Data e hora de criação do token.
     * <p>
     * Configurado automaticamente via {@link #onCreate()} antes da primeira persistência.
     * Campo não pode ser atualizado após criação ({@code updatable = false}).
     * </p>
     * <p>
     * Útil para:
     * <ul>
     *   <li>Auditoria de quando tokens foram emitidos</li>
     *   <li>Limpeza de tokens muito antigos</li>
     *   <li>Análise de padrões de uso</li>
     * </ul>
     * </p>
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Callback JPA executado automaticamente antes de persistir a entidade pela primeira vez.
     * <p>
     * Define {@link #createdAt} com o timestamp atual, garantindo que toda
     * entidade tenha data de criação registrada.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Construtor padrão protegido.
     * <p>
     * Requerido pelo JPA para criação de proxies e instanciação via reflection.
     * Protegido (não público) para evitar criação direta de instâncias vazias
     * fora do contexto JPA.
     * </p>
     */
    protected AdminRefreshToken() {}

    /**
     * Retorna o identificador único do token.
     *
     * @return UUID do token no banco de dados
     */
    public UUID getId() {
        return id;
    }

    /**
     * Define o identificador único do token.
     * <p>
     * Geralmente não deve ser chamado manualmente, pois o ID é gerado
     * automaticamente pelo JPA.
     * </p>
     *
     * @param id novo UUID do token
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Retorna a string do token usada nas requisições de refresh.
     *
     * @return string do token (UUID gerado aleatoriamente)
     */
    public String getToken() {
        return token;
    }

    /**
     * Define a string do token.
     * <p>
     * Deve ser um valor único e imprevisível. Tipicamente um UUID aleatório.
     * </p>
     *
     * @param token string do refresh token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Retorna o administrador proprietário deste refresh token.
     * <p>
     * <b>Atenção:</b> Como o relacionamento usa {@code FetchType.LAZY}, acessar
     * este método fora de uma transação ativa pode resultar em
     * {@code LazyInitializationException}.
     * </p>
     *
     * @return entidade do admin associado ao token
     */
    public AdminEntity getAdmin() {
        return admin;
    }

    /**
     * Associa este refresh token a um administrador.
     *
     * @param admin entidade do admin que será proprietário do token
     */
    public void setAdmin(AdminEntity admin) {
        this.admin = admin;
    }

    /**
     * Retorna a data e hora de expiração do token.
     *
     * @return timestamp indicando quando o token expira
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Define a data e hora de expiração do token.
     * <p>
     * Tipicamente configurado para 7 dias após a criação, mas pode variar
     * conforme política de segurança da aplicação.
     * </p>
     *
     * @param expiresAt timestamp de expiração do token
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Verifica se o token foi revogado.
     *
     * @return {@code true} se o token foi revogado e não pode mais ser usado,
     *         {@code false} se ainda está ativo
     */
    public boolean isRevoked() {
        return revoked;
    }

    /**
     * Define o status de revogação do token.
     * <p>
     * Uma vez revogado ({@code true}), o token não pode mais ser usado para
     * gerar novos access tokens, mesmo que ainda não tenha expirado.
     * </p>
     * <p>
     * <b>Importante:</b> Revogação é uma operação irreversível. Após revogar
     * um token, ele permanece revogado permanentemente.
     * </p>
     *
     * @param revoked {@code true} para revogar o token, {@code false} para mantê-lo ativo
     */
    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * Retorna a data e hora de criação do token.
     * <p>
     * Este valor é definido automaticamente via {@link #onCreate()} e não pode
     * ser alterado após a criação.
     * </p>
     *
     * @return timestamp de quando o token foi criado
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}