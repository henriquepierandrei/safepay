package tech.safepay.entities;

import jakarta.persistence.*;
import tech.safepay.Enums.MerchantCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa o padrão comportamental de um cartão.
 * <p>
 * Armazena métricas estatísticas e hábitos de uso utilizados
 * pelo motor antifraude para detecção de anomalias.
 *
 * Este modelo consolida histórico de transações e serve como
 * baseline para comparação em tempo de execução.
 *
 * Responsabilidades:
 * <ul>
 *   <li>Manter vínculo 1:1 com um cartão</li>
 *   <li>Registrar valores médios e máximos de transação</li>
 *   <li>Mapear categorias e localidades frequentes</li>
 *   <li>Armazenar padrões temporais de uso</li>
 * </ul>
 *
 * Não executa validações de fraude diretamente.
 * Atua como entidade de apoio ao pipeline antifraude.
 *
 * @author SafePay Team
 * @version 1.0
 */
@Entity
@Table(name = "cards_patterns_tb")
public class CardPattern {

    /**
     * Identificador único do padrão comportamental.
     */
    @Id
    @GeneratedValue
    private UUID patternId;

    /**
     * Cartão associado ao padrão.
     * Relação um-para-um.
     */
    @OneToOne
    @JoinColumn(name = "card_id")
    private Card card;

    /**
     * Valor médio das transações realizadas.
     */
    private BigDecimal avgTransactionAmount;

    /**
     * Maior valor já registrado em uma transação.
     */
    private BigDecimal maxTransactionAmount;

    /**
     * Categorias de comerciantes mais frequentes.
     */
    @Enumerated(EnumType.STRING)
    private List<MerchantCategory> commonCategories;

    /**
     * Localizações mais recorrentes das transações.
     */
    private List<String> commonLocations;

    /**
     * Frequência média de transações por dia.
     */
    private Integer transactionFrequencyPerDay;

    /**
     * Horários preferenciais de uso do cartão.
     */
    private List<LocalDateTime> preferredHours;

    /**
     * Data da última atualização do padrão.
     */
    private LocalDateTime lastUpdated;

    /** Construtor padrão exigido pelo JPA */
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
