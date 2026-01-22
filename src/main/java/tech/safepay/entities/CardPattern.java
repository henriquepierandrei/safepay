package tech.safepay.entities;


import jakarta.persistence.*;
import tech.safepay.Enums.MerchantCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    // getters, setters, constructors

    public CardPattern() {
    }

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

    public List<String> getCommonLocations() {
        return commonLocations;
    }

    public void setCommonLocations(List<String> commonLocations) {
        this.commonLocations = commonLocations;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<MerchantCategory> getCommonCategories() {
        return commonCategories;
    }

    public void setCommonCategories(List<MerchantCategory> commonCategories) {
        this.commonCategories = commonCategories;
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
}
