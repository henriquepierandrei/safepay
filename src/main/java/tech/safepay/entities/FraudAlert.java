package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.converter.AlertTypeConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um alerta de fraude gerado pelo sistema antifraude.
 * <p>
 * O alerta consolida informações resultantes da análise de uma transação,
 * incluindo tipos de alerta disparados, severidade e score de risco.
 *
 * Este objeto é utilizado para:
 * <ul>
 *   <li>Auditoria de decisões antifraude</li>
 *   <li>Monitoramento de padrões suspeitos</li>
 *   <li>Suporte a análises e investigações</li>
 * </ul>
 *
 * Não executa regras de negócio; representa o resultado
 * da avaliação realizada pelo pipeline antifraude.
 *
 * @author SafePay Team
 * @version 1.0
 */
@Entity
@Table(name = "fraud_alerts_tb")
public class FraudAlert {

    /**
     * Identificador único do alerta de fraude.
     */
    @Id
    @GeneratedValue
    private UUID alertId;

    /**
     * Transação associada ao alerta.
     */
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    /**
     * Cartão relacionado ao alerta de fraude.
     */
    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;

    /**
     * Tipos de alerta disparados durante a análise.
     * Persistidos como lista serializada.
     */
    @Convert(converter = AlertTypeConverter.class)
    @Column(name = "alert_types", length = 500)
    private List<AlertType> alertTypes;

    /**
     * Severidade do alerta.
     */
    @Enumerated(EnumType.STRING)
    private Severity severity;

    /**
     * Probabilidade estimada de fraude.
     */
    private Integer fraudProbability;

    /**
     * Descrição detalhada do motivo do alerta.
     */
    private String description;

    /**
     * Status atual do alerta.
     */
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    /**
     * Data de criação do alerta.
     */
    private LocalDateTime createdAt;

    /**
     * Score final de fraude calculado.
     */
    private Integer fraudScore;

    /** Construtor padrão exigido pelo JPA */
    public FraudAlert() {
    }


    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public List<AlertType> getAlertTypes() {
        return alertTypes;
    }

    public void setAlertTypes(List<AlertType> alertTypes) {
        this.alertTypes = alertTypes;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Integer getFraudProbability() {
        return fraudProbability;
    }

    public void setFraudProbability(Integer fraudProbability) {
        this.fraudProbability = fraudProbability;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Integer fraudScore) {
        this.fraudScore = fraudScore;
    }
}
