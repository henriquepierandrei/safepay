package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.DeviceType;
import tech.safepay.Enums.MerchantCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions_tb")
public class Transaction {

    @Id
    @GeneratedValue
    private UUID transactionId;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    private MerchantCategory merchantCategory;

    private BigDecimal amount;

    private LocalDateTime transactionDateAndTime;

    @Column(length = 100)
        private String latitude;

    @Column(length = 100)
    private String longitude;


    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    private String ipAdress;

    private Boolean status;
    private Boolean isFraud;
    private Integer fraudScore;

    // getters, setters, constructors

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

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getIpAdress() {
        return ipAdress;
    }

    public void setIpAdress(String ipAdress) {
        this.ipAdress = ipAdress;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getFraud() {
        return isFraud;
    }

    public void setFraud(Boolean fraud) {
        isFraud = fraud;
    }

    public Integer getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Integer fraudScore) {
        this.fraudScore = fraudScore;
    }
}
