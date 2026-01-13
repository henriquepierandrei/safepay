package tech.safepay.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dashboard_analytics_db")
public class DashboardAnalytics {

    @Id
    @GeneratedValue
    private UUID statId;

    private LocalDate date;
    private Integer totalTransactions;
    private BigDecimal totalAmount;
    private Integer fraudTransactions;
    private Integer fraudAmount;
    private Integer fraudDetectionRate;
    private Integer falseDetectionRate;
    private Integer falsePositiveRate;

    // getters, setters, constructors

    public DashboardAnalytics() {
    }

    public UUID getStatId() {
        return statId;
    }

    public void setStatId(UUID statId) {
        this.statId = statId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Integer totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getFraudTransactions() {
        return fraudTransactions;
    }

    public void setFraudTransactions(Integer fraudTransactions) {
        this.fraudTransactions = fraudTransactions;
    }

    public Integer getFraudAmount() {
        return fraudAmount;
    }

    public void setFraudAmount(Integer fraudAmount) {
        this.fraudAmount = fraudAmount;
    }

    public Integer getFraudDetectionRate() {
        return fraudDetectionRate;
    }

    public void setFraudDetectionRate(Integer fraudDetectionRate) {
        this.fraudDetectionRate = fraudDetectionRate;
    }

    public Integer getFalseDetectionRate() {
        return falseDetectionRate;
    }

    public void setFalseDetectionRate(Integer falseDetectionRate) {
        this.falseDetectionRate = falseDetectionRate;
    }

    public Integer getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public void setFalsePositiveRate(Integer falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
    }
}
