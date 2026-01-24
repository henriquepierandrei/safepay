package tech.safepay.ml;

import jakarta.persistence.*;
import tech.safepay.Enums.TransactionDecision;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_training_tb")
public class FraudTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    // ALERTAS (one-hot)
    private boolean highAmount;
    private boolean limitExceeded;
    private boolean velocityAbuse;
    private boolean burstActivity;
    private boolean locationAnomaly;
    private boolean impossibleTravel;
    private boolean highRiskCountry;
    private boolean newDeviceDetected;
    private boolean deviceFingerprintChange;
    private boolean torOrProxyDetected;
    private boolean multipleCardsSameDevice;
    private boolean timeOfDayAnomaly;
    private boolean cardTesting;
    private boolean microTransactionPattern;
    private boolean declineThenApprovePattern;
    private boolean multipleFailedAttempts;
    private boolean suspiciousSuccessAfterFailure;
    private boolean anomalyModelTriggered;
    private boolean creditLimitReached;
    private boolean expirationDateApproaching;

    // FEATURES NUMÉRICAS
    private int alertCount;
    private int riskScore;
    private int maxAlertScore;

    // LABEL
    @Enumerated(EnumType.STRING)
    private TransactionDecision finalDecision;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isHighAmount() {
        return highAmount;
    }

    public void setHighAmount(boolean highAmount) {
        this.highAmount = highAmount;
    }

    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    public void setLimitExceeded(boolean limitExceeded) {
        this.limitExceeded = limitExceeded;
    }

    public boolean isVelocityAbuse() {
        return velocityAbuse;
    }

    public void setVelocityAbuse(boolean velocityAbuse) {
        this.velocityAbuse = velocityAbuse;
    }

    public boolean isBurstActivity() {
        return burstActivity;
    }

    public void setBurstActivity(boolean burstActivity) {
        this.burstActivity = burstActivity;
    }

    public boolean isLocationAnomaly() {
        return locationAnomaly;
    }

    public void setLocationAnomaly(boolean locationAnomaly) {
        this.locationAnomaly = locationAnomaly;
    }

    public boolean isImpossibleTravel() {
        return impossibleTravel;
    }

    public void setImpossibleTravel(boolean impossibleTravel) {
        this.impossibleTravel = impossibleTravel;
    }

    public boolean isHighRiskCountry() {
        return highRiskCountry;
    }

    public void setHighRiskCountry(boolean highRiskCountry) {
        this.highRiskCountry = highRiskCountry;
    }

    public boolean isNewDeviceDetected() {
        return newDeviceDetected;
    }

    public void setNewDeviceDetected(boolean newDeviceDetected) {
        this.newDeviceDetected = newDeviceDetected;
    }

    public boolean isDeviceFingerprintChange() {
        return deviceFingerprintChange;
    }

    public void setDeviceFingerprintChange(boolean deviceFingerprintChange) {
        this.deviceFingerprintChange = deviceFingerprintChange;
    }

    public boolean isTorOrProxyDetected() {
        return torOrProxyDetected;
    }

    public void setTorOrProxyDetected(boolean torOrProxyDetected) {
        this.torOrProxyDetected = torOrProxyDetected;
    }

    public boolean isMultipleCardsSameDevice() {
        return multipleCardsSameDevice;
    }

    public void setMultipleCardsSameDevice(boolean multipleCardsSameDevice) {
        this.multipleCardsSameDevice = multipleCardsSameDevice;
    }

    public boolean isTimeOfDayAnomaly() {
        return timeOfDayAnomaly;
    }

    public void setTimeOfDayAnomaly(boolean timeOfDayAnomaly) {
        this.timeOfDayAnomaly = timeOfDayAnomaly;
    }

    public boolean isCardTesting() {
        return cardTesting;
    }

    public void setCardTesting(boolean cardTesting) {
        this.cardTesting = cardTesting;
    }

    public boolean isMicroTransactionPattern() {
        return microTransactionPattern;
    }

    public void setMicroTransactionPattern(boolean microTransactionPattern) {
        this.microTransactionPattern = microTransactionPattern;
    }

    public boolean isDeclineThenApprovePattern() {
        return declineThenApprovePattern;
    }

    public void setDeclineThenApprovePattern(boolean declineThenApprovePattern) {
        this.declineThenApprovePattern = declineThenApprovePattern;
    }

    public boolean isMultipleFailedAttempts() {
        return multipleFailedAttempts;
    }

    public void setMultipleFailedAttempts(boolean multipleFailedAttempts) {
        this.multipleFailedAttempts = multipleFailedAttempts;
    }

    public boolean isSuspiciousSuccessAfterFailure() {
        return suspiciousSuccessAfterFailure;
    }

    public void setSuspiciousSuccessAfterFailure(boolean suspiciousSuccessAfterFailure) {
        this.suspiciousSuccessAfterFailure = suspiciousSuccessAfterFailure;
    }

    public boolean isAnomalyModelTriggered() {
        return anomalyModelTriggered;
    }

    public void setAnomalyModelTriggered(boolean anomalyModelTriggered) {
        this.anomalyModelTriggered = anomalyModelTriggered;
    }

    public boolean isCreditLimitReached() {
        return creditLimitReached;
    }

    public void setCreditLimitReached(boolean creditLimitReached) {
        this.creditLimitReached = creditLimitReached;
    }

    public boolean isExpirationDateApproaching() {
        return expirationDateApproaching;
    }

    public void setExpirationDateApproaching(boolean expirationDateApproaching) {
        this.expirationDateApproaching = expirationDateApproaching;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public int getMaxAlertScore() {
        return maxAlertScore;
    }

    public void setMaxAlertScore(int maxAlertScore) {
        this.maxAlertScore = maxAlertScore;
    }

    public TransactionDecision getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(TransactionDecision finalDecision) {
        this.finalDecision = finalDecision;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}