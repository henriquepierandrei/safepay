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
}
