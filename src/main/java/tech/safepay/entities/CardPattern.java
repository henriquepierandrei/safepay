package tech.safepay.entities;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(columnDefinition = "json")
    private String commonCategories;

    @Column(columnDefinition = "json")
    private String commonLocations;

    private Integer transactionFrequency;

    @Column(columnDefinition = "json")
    private String preferredHours;

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

    public String getCommonCategories() {
        return commonCategories;
    }

    public void setCommonCategories(String commonCategories) {
        this.commonCategories = commonCategories;
    }

    public String getCommonLocations() {
        return commonLocations;
    }

    public void setCommonLocations(String commonLocations) {
        this.commonLocations = commonLocations;
    }

    public Integer getTransactionFrequency() {
        return transactionFrequency;
    }

    public void setTransactionFrequency(Integer transactionFrequency) {
        this.transactionFrequency = transactionFrequency;
    }

    public String getPreferredHours() {
        return preferredHours;
    }

    public void setPreferredHours(String preferredHours) {
        this.preferredHours = preferredHours;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
