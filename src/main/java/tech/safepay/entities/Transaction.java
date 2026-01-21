package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.DeviceType;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.TransactionStatus;

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

    @Column(length = 100)
    private String countryCode;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;


    @ManyToOne(optional = false)
    private Device device;


    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    private Boolean isFraud;

    private LocalDateTime createdAt;

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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
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
}
