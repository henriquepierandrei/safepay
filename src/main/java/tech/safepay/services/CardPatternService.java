package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.entities.CardPattern;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardPatternRepository;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CardPatternService {

    private final CardPatternRepository cardPatternRepository;
    private final TransactionRepository transactionRepository;

    // Constantes para análise comportamental
    private static final int MIN_TRANSACTIONS_FOR_PATTERN = 10;
    private static final int PERCENTILE_95 = 95;
    private static final int TOP_MERCHANTS_LIMIT = 10;
    private static final int HOURLY_BUCKETS = 24;

    public CardPatternService(CardPatternRepository cardPatternRepository,
                              TransactionRepository transactionRepository) {
        this.cardPatternRepository = cardPatternRepository;
        this.transactionRepository = transactionRepository;
    }

    public CardPattern buildOrUpdateCardPattern(Card card) {
        var transactions = transactionRepository.findByCard(card);

        if (transactions.isEmpty()) {
            return cardPatternRepository.findByCard(card)
                    .orElseGet(() -> {
                        CardPattern p = new CardPattern();
                        p.setCard(card);
                        return cardPatternRepository.save(p);
                    });
        }

        CardPattern pattern = cardPatternRepository.findByCard(card)
                .orElseGet(() -> {
                    CardPattern p = new CardPattern();
                    p.setCard(card);
                    return p;
                });

        // ===== ANÁLISE DE VALORES =====
        analyzeTransactionAmounts(pattern, transactions);

        // ===== ANÁLISE DE CATEGORIAS =====
        analyzeCategories(pattern, transactions);

        // ===== ANÁLISE TEMPORAL AVANÇADA =====
        analyzeTemporalPatterns(pattern, transactions);

        // ===== ANÁLISE GEOGRÁFICA E COMERCIANTES =====
        analyzeMerchantsAndLocations(pattern, transactions);

        // ===== ANÁLISE DE COMPORTAMENTO ANÔMALO =====
        analyzeBehavioralMetrics(pattern, transactions);

        pattern.setLastUpdated(LocalDateTime.now());
        return cardPatternRepository.save(pattern);
    }

    /**
     * Análise estatística completa dos valores transacionados
     */
    private void analyzeTransactionAmounts(CardPattern pattern, List<Transaction> transactions) {
        List<BigDecimal> amounts = transactions.stream()
                .map(Transaction::getAmount)
                .sorted()
                .toList();

        // Média e total
        BigDecimal total = amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);

        pattern.setAvgTransactionAmount(avg);
        pattern.setMaxTransactionAmount(amounts.get(amounts.size() - 1));

        // Mediana (valor central - mais resistente a outliers)
        BigDecimal median = amounts.get(amounts.size() / 2);

        // Desvio padrão (mede a variabilidade dos gastos)
        BigDecimal variance = amounts.stream()
                .map(a -> a.subtract(avg).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Percentil 95 (valores normais vão até aqui)
        int p95Index = (int) Math.ceil(amounts.size() * 0.95) - 1;
        BigDecimal percentile95 = amounts.get(Math.min(p95Index, amounts.size() - 1));

        // Quartis para detectar padrões de gasto
        BigDecimal q1 = amounts.get(amounts.size() / 4);
        BigDecimal q3 = amounts.get(3 * amounts.size() / 4);

        // IQR (Interquartile Range) - usado para detectar outliers
        BigDecimal iqr = q3.subtract(q1);

        // Classificação de tickets
        Map<String, Long> ticketClassification = classifyTickets(amounts, q1, median, q3);

        // Armazena métricas adicionais (você pode adicionar campos no CardPattern)
        // pattern.setMedianAmount(median);
        // pattern.setStdDevAmount(stdDev);
        // pattern.setPercentile95Amount(percentile95);
        // pattern.setTicketClassification(ticketClassification);
    }

    /**
     * Classifica transações por tamanho de ticket
     */
    private Map<String, Long> classifyTickets(List<BigDecimal> amounts, BigDecimal q1,
                                              BigDecimal median, BigDecimal q3) {
        return amounts.stream()
                .collect(Collectors.groupingBy(
                        amount -> {
                            if (amount.compareTo(q1) < 0) return "micro";
                            if (amount.compareTo(median) < 0) return "pequeno";
                            if (amount.compareTo(q3) < 0) return "médio";
                            return "grande";
                        },
                        Collectors.counting()
                ));
    }

    /**
     * Análise detalhada de categorias de comerciantes
     */
    private void analyzeCategories(CardPattern pattern, List<Transaction> transactions) {
        // Categorias mais frequentes com seus percentuais
        Map<MerchantCategory, Long> categoryFrequency = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getMerchantCategory,
                        Collectors.counting()
                ));

        List<MerchantCategory> topCategories = categoryFrequency.entrySet().stream()
                .sorted(Map.Entry.<MerchantCategory, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        pattern.setCommonCategories(topCategories);

        // Análise de gasto por categoria
        Map<MerchantCategory, BigDecimal> spendingByCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getMerchantCategory,
                        Collectors.mapping(
                                Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Identifica categoria com maior gasto médio
        Map<MerchantCategory, BigDecimal> avgByCategory = categoryFrequency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> spendingByCategory.get(e.getKey())
                                .divide(BigDecimal.valueOf(e.getValue()), 2, RoundingMode.HALF_UP)
                ));

        // Diversificação de categorias (índice de entropia)
        double categoryEntropy = calculateEntropy(
                categoryFrequency.values().stream()
                        .map(Long::doubleValue)
                        .toList()
        );

        // pattern.setCategoryDiversity(categoryEntropy);
        // pattern.setSpendingByCategory(spendingByCategory);
    }

    /**
     * Análise temporal sofisticada
     */
    private void analyzeTemporalPatterns(CardPattern pattern, List<Transaction> transactions) {
        // Análise por hora do dia (distribuição em 24 buckets)
        Map<Integer, Long> hourlyDistribution = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().getHour(),
                        Collectors.counting()
                ));

        // Top 3 horários mais frequentes
        List<Integer> topHours = hourlyDistribution.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // Converte para LocalDateTime (pode melhorar armazenando apenas as horas)
        List<LocalDateTime> preferredHours = topHours.stream()
                .map(hour -> LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0))
                .toList();

        pattern.setPreferredHours(preferredHours);

        // Análise por dia da semana
        Map<DayOfWeek, Long> weekdayDistribution = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().getDayOfWeek(),
                        Collectors.counting()
                ));

        // Dias da semana preferidos
        List<DayOfWeek> preferredDays = weekdayDistribution.entrySet().stream()
                .sorted(Map.Entry.<DayOfWeek, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // Padrão weekend vs weekday
        long weekendTxns = transactions.stream()
                .filter(t -> {
                    DayOfWeek day = t.getTransactionDateAndTime().getDayOfWeek();
                    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                })
                .count();

        double weekendRatio = (double) weekendTxns / transactions.size();

        // Frequência de transações por dia
        Map<String, Long> dailyFrequency = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().toLocalDate().toString(),
                        Collectors.counting()
                ));

        int avgDailyFrequency = (int) Math.round(
                dailyFrequency.values().stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0)
        );

        pattern.setTransactionFrequencyPerDay(avgDailyFrequency);

        // Análise de intervalos entre transações
        List<Long> intervalsBetweenTransactions = calculateIntervals(transactions);
        double avgInterval = intervalsBetweenTransactions.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        // pattern.setPreferredWeekdays(preferredDays);
        // pattern.setWeekendTransactionRatio(weekendRatio);
        // pattern.setAvgIntervalBetweenTransactions(avgInterval); // em minutos
    }

    /**
     * Calcula intervalos de tempo entre transações consecutivas
     */
    private List<Long> calculateIntervals(List<Transaction> transactions) {
        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDateAndTime))
                .toList();

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            long minutes = ChronoUnit.MINUTES.between(
                    sorted.get(i - 1).getTransactionDateAndTime(),
                    sorted.get(i).getTransactionDateAndTime()
            );
            intervals.add(minutes);
        }
        return intervals;
    }

    /**
     * Análise de comerciantes e localizações
     */
    private void analyzeMerchantsAndLocations(CardPattern pattern, List<Transaction> transactions) {
        // Top comerciantes por frequência
        Map<String, Long> merchantFrequency = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getMerchantCategory().toString() != null ? t.getMerchantCategory().toString() : "UNKNOWN",
                        Collectors.counting()
                ));

        List<String> topMerchants = merchantFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_MERCHANTS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        // Análise de localização (se disponível)
        // Conta quantas cidades/países diferentes
        Set<String> uniqueLocations = transactions.stream()
                .map(t -> t.getCity() != null ? t.getCity() : "UNKNOWN")
                .collect(Collectors.toSet());

        int locationDiversity = uniqueLocations.size();

        // pattern.setTopMerchants(topMerchants);
        // pattern.setLocationDiversity(locationDiversity);
    }

    /**
     * Métricas comportamentais para detecção de anomalias
     */
    private void analyzeBehavioralMetrics(CardPattern pattern, List<Transaction> transactions) {
        // Taxa de transações recusadas (se houver esse campo)
        // long declinedCount = transactions.stream()
        //         .filter(t -> "DECLINED".equals(t.getStatus()))
        //         .count();
        // double declineRate = (double) declinedCount / transactions.size();

        // Velocidade de transações (transações por hora em períodos ativos)
        Map<String, Long> hourlyActivity = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().toLocalDate().toString() + "-" +
                                t.getTransactionDateAndTime().getHour(),
                        Collectors.counting()
                ));

        double maxTransactionsPerHour = hourlyActivity.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        // Consistência temporal (desvio padrão dos horários)
        List<Integer> hours = transactions.stream()
                .map(t -> t.getTransactionDateAndTime().getHour())
                .toList();

        double avgHour = hours.stream().mapToInt(Integer::intValue).average().orElse(12);
        double hourStdDev = Math.sqrt(
                hours.stream()
                        .mapToDouble(h -> Math.pow(h - avgHour, 2))
                        .average()
                        .orElse(0)
        );

        // pattern.setMaxTransactionsPerHour(maxTransactionsPerHour);
        // pattern.setTemporalConsistency(hourStdDev); // menor = mais consistente
    }

    /**
     * Calcula entropia (medida de diversidade/aleatoriedade)
     */
    private double calculateEntropy(List<Double> distribution) {
        double total = distribution.stream().mapToDouble(Double::doubleValue).sum();
        return distribution.stream()
                .mapToDouble(count -> {
                    if (count == 0) return 0;
                    double p = count / total;
                    return -p * (Math.log(p) / Math.log(2));
                })
                .sum();
    }
}