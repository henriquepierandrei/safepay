package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.TransactionDecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma transação processada pelo SafePay.
 * <p>
 * Consolida dados financeiros, contexto geográfico, dispositivo
 * utilizado e o resultado da decisão antifraude.
 *
 * Esta entidade é o núcleo do pipeline antifraude e serve como
 * base para geração de alertas, cálculo de score e auditoria.
 *
 * Responsabilidades:
 * <ul>
 *   <li>Registrar informações financeiras da transação</li>
 *   <li>Associar cartão e dispositivo envolvidos</li>
 *   <li>Persistir dados de localização e IP</li>
 *   <li>Armazenar decisão e classificação de fraude</li>
 * </ul>
 *
 * Não executa validações; apenas representa o resultado
 * do processamento realizado pelo pipeline antifraude.
 *
 * @author SafePay Team
 * @version 1.0
 */
@Entity
@Table(name = "transactions_tb")
public class Transaction {

    /**
     * Identificador único da transação.
     */
    @Id
    @GeneratedValue
    private UUID transactionId;

    /**
     * Cartão utilizado na transação.
     */
    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /**
     * Categoria do comerciante.
     */
    @Enumerated(EnumType.STRING)
    private MerchantCategory merchantCategory;

    /**
     * Valor monetário da transação.
     */
    private BigDecimal amount;

    /**
     * Data e hora da transação.
     */
    private LocalDateTime transactionDateAndTime;

    /**
     * Latitude geográfica da transação.
     */
    @Column(length = 100)
    private String latitude;

    /**
     * Longitude geográfica da transação.
     */
    @Column(length = 100)
    private String longitude;

    /**
     * Código do país identificado.
     */
    @Column(length = 100)
    private String countryCode;

    /**
     * Estado identificado na transação.
     */
    @Column(length = 100)
    private String state;

    /**
     * Cidade identificada na transação.
     */
    @Column(length = 100)
    private String city;

    /**
     * Dispositivo utilizado na transação.
     */
    @ManyToOne(optional = false)
    private Device device;

    /**
     * Fingerprint do dispositivo no momento da transação.
     */
    @Column(nullable = false)
    private String deviceFingerprint;

    /**
     * Endereço IP de origem da transação.
     */
    private String ipAddress;

    /**
     * Decisão final do pipeline antifraude.
     */
    @Enumerated(EnumType.STRING)
    private TransactionDecision transactionDecision;

    /**
     * Indica se a transação foi classificada como fraude.
     */
    private Boolean isFraud;

    /**
     * Indica se a transação foi reembolsada.
     */
    private Boolean isReimbursement;

    /**
     * Data de criação do registro.
     */
    private LocalDateTime createdAt;

    /** Construtor padrão exigido pelo JPA */
    public Transaction() {
    }


    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public MerchantCategory getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(MerchantCategory merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDateAndTime() {
        return transactionDateAndTime;
    }

    public void setTransactionDateAndTime(LocalDateTime transactionDateAndTime) {
        this.transactionDateAndTime = transactionDateAndTime;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public TransactionDecision getTransactionDecision() {
        return transactionDecision;
    }

    public void setTransactionDecision(TransactionDecision transactionDecision) {
        this.transactionDecision = transactionDecision;
    }

    public Boolean getFraud() {
        return isFraud;
    }

    public void setFraud(Boolean fraud) {
        isFraud = fraud;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public Boolean getReimbursement() {
        return isReimbursement;
    }

    public void setReimbursement(Boolean reimbursement) {
        isReimbursement = reimbursement;
    }
}
