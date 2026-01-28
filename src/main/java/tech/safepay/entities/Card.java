package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um cartão no domínio do SafePay.
 * <p>
 * Armazena informações financeiras, status operacional e vínculos
 * com dispositivos utilizados em transações antifraude.
 *
 * Este objeto é persistido no banco de dados e participa
 * diretamente do pipeline de avaliação de risco.
 *
 * Responsabilidades:
 * <ul>
 *   <li>Identificar o cartão de forma única</li>
 *   <li>Manter vínculo com dispositivos utilizados</li>
 *   <li>Armazenar limites, status e score de risco</li>
 *   <li>Registrar datas relevantes para auditoria</li>
 * </ul>
 *
 * Não contém regras de negócio complexas; atua como
 * modelo de persistência e leitura.
 *
 * @author SafePay Team
 * @version 1.0
 */
@Entity
@Table(name = "cards_tb")
public class Card {

    /**
     * Identificador único do cartão.
     */
    @Id
    @GeneratedValue
    private UUID cardId;

    /**
     * Número do cartão (armazenamento depende de política de segurança).
     */
    private String cardNumber;

    /**
     * Nome do portador do cartão.
     */
    private String cardHolderName;

    /**
     * Bandeira do cartão.
     */
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand;

    /**
     * Dispositivos vinculados ao cartão.
     * Relação muitos-para-muitos.
     */
    @ManyToMany
    @JoinTable(
            name = "card_devices",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "device_id")
    )
    private List<Device> devices = new ArrayList<>();

    /**
     * Data de expiração do cartão.
     */
    private LocalDate expirationDate;

    /**
     * Limite total de crédito.
     */
    private BigDecimal creditLimit;

    /**
     * Limite disponível no momento.
     */
    private BigDecimal remainingLimit;

    /**
     * Status atual do cartão.
     */
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    /**
     * Score de risco acumulado do cartão.
     */
    private Integer riskScore;

    /**
     * Data de criação do registro.
     */
    private LocalDateTime createdAt;

    /**
     * Data da última transação realizada.
     */
    private LocalDateTime lastTransactionAt;



    /** Construtor padrão exigido pelo JPA */
    public Card() {
    }

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public CardBrand getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(CardBrand cardBrand) {
        this.cardBrand = cardBrand;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public BigDecimal getRemainingLimit() {
        return remainingLimit;
    }

    public void setRemainingLimit(BigDecimal remainingLimit) {
        this.remainingLimit = remainingLimit;
    }

    public LocalDateTime getLastTransactionAt() {
        return lastTransactionAt;
    }

    public void setLastTransactionAt(LocalDateTime lastTransactionAt) {
        this.lastTransactionAt = lastTransactionAt;
    }

}
