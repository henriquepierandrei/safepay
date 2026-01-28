package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.MerchantCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa o padrão comportamental de um cartão.
 * Armazena métricas estatísticas e hábitos de uso utilizados
 * pelo motor antifraude para detecção de anomalias.
 */
@Entity
@Table(name = "cards_patterns_tb")
public class CardPattern {

    @Id
    @GeneratedValue
    private UUID patternId;

    @OneToOne
    @JoinColumn(name = "card_id")
    private Card card;

    private BigDecimal avgTransactionAmount;
    private BigDecimal maxTransactionAmount;

    @Enumerated(EnumType.STRING)
    private List<MerchantCategory> commonCategories;

    private List<String> commonLocations;
    private Integer transactionFrequencyPerDay;
    private List<LocalDateTime> preferredHours;
    private LocalDateTime lastUpdated;

    // ===== NOVOS CAMPOS ESTATÍSTICOS =====
    private BigDecimal medianAmount;          // Mediana
    private BigDecimal stdDevAmount;          // Desvio padrão
    private BigDecimal percentile95Amount;    // Percentil 95
    private BigDecimal q1Amount;              // Primeiro quartil (25%)
    private BigDecimal q3Amount;              // Terceiro quartil (75%)
    private BigDecimal iqrAmount;             // Intervalo interquartil (q3 - q1)


    /** Construtor padrão exigido pelo JPA */
    public CardPattern() {}

    // ===== GETTERS E SETTERS =====

    public UUID getPatternId() {
        return patternId;
    }

    public void setPatternId(UUID patternId) {
        this.patternId = patternId;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public BigDecimal getAvgTransactionAmount() {
        return avgTransactionAmount;
    }

    public void setAvgTransactionAmount(BigDecimal avgTransactionAmount) {
        this.avgTransactionAmount = avgTransactionAmount;
    }

    public BigDecimal getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public void setMaxTransactionAmount(BigDecimal maxTransactionAmount) {
        this.maxTransactionAmount = maxTransactionAmount;
    }

    public List<MerchantCategory> getCommonCategories() {
        return commonCategories;
    }

    public void setCommonCategories(List<MerchantCategory> commonCategories) {
        this.commonCategories = commonCategories;
    }

    public List<String> getCommonLocations() {
        return commonLocations;
    }

    public void setCommonLocations(List<String> commonLocations) {
        this.commonLocations = commonLocations;
    }

    public Integer getTransactionFrequencyPerDay() {
        return transactionFrequencyPerDay;
    }

    public void setTransactionFrequencyPerDay(Integer transactionFrequencyPerDay) {
        this.transactionFrequencyPerDay = transactionFrequencyPerDay;
    }

    public List<LocalDateTime> getPreferredHours() {
        return preferredHours;
    }

    public void setPreferredHours(List<LocalDateTime> preferredHours) {
        this.preferredHours = preferredHours;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public BigDecimal getMedianAmount() {
        return medianAmount;
    }

    public void setMedianAmount(BigDecimal medianAmount) {
        this.medianAmount = medianAmount;
    }

    public BigDecimal getStdDevAmount() {
        return stdDevAmount;
    }

    public void setStdDevAmount(BigDecimal stdDevAmount) {
        this.stdDevAmount = stdDevAmount;
    }

    public BigDecimal getPercentile95Amount() {
        return percentile95Amount;
    }

    public void setPercentile95Amount(BigDecimal percentile95Amount) {
        this.percentile95Amount = percentile95Amount;
    }

    public BigDecimal getQ1Amount() {
        return q1Amount;
    }

    public void setQ1Amount(BigDecimal q1Amount) {
        this.q1Amount = q1Amount;
    }

    public BigDecimal getQ3Amount() {
        return q3Amount;
    }

    public void setQ3Amount(BigDecimal q3Amount) {
        this.q3Amount = q3Amount;
    }

    public BigDecimal getIqrAmount() {
        return iqrAmount;
    }

    public void setIqrAmount(BigDecimal iqrAmount) {
        this.iqrAmount = iqrAmount;
    }

}
