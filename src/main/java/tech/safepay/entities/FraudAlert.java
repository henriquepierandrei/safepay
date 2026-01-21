package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts_tb")
public class FraudAlert {

    @Id
    @GeneratedValue
    private UUID alertId;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;


    @Enumerated(EnumType.STRING)
    private List<AlertType> alertTypes;


    @Enumerated(EnumType.STRING)
    private Severity severity;

    private Integer fraudProbability;
    private String description;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private LocalDateTime createdAt;

    private Integer fraudScore;

    // getters, setters, constructors

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
