package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela análise e construção de padrões comportamentais de cartões de crédito.
 * <p>
 * Este serviço realiza análises estatísticas e comportamentais avançadas sobre o histórico de transações
 * de cartões, incluindo:
 * <ul>
 *   <li>Análise estatística de valores transacionados (média, mediana, desvio padrão, percentis)</li>
 *   <li>Análise de categorias de comerciantes e padrões de gasto</li>
 *   <li>Análise temporal (horários, dias da semana, frequência)</li>
 *   <li>Análise geográfica e de comerciantes</li>
 *   <li>Métricas comportamentais para detecção de anomalias</li>
 * </ul>
 * <p>
 * Os padrões construídos são utilizados para detecção de fraudes e análise de comportamento de usuários.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@Transactional
public class CardPatternService {

    private static final Logger log = LoggerFactory.getLogger(CardPatternService.class);
    private final CardPatternRepository cardPatternRepository;
    private final TransactionRepository transactionRepository;

    // Constantes para análise comportamental

    /**
     * Threshold para cálculo do percentil 95 (0.95 = 95%).
     */
    private static final double PERCENTILE_95_THRESHOLD = 0.95;

    /**
     * Limite de comerciantes principais a serem considerados na análise.
     */
    private static final int TOP_MERCHANTS_LIMIT = 10;

    /**
     * Limite de categorias principais a serem consideradas na análise.
     */
    private static final int TOP_CATEGORIES_LIMIT = 5;

    /**
     * Limite de horários principais a serem considerados na análise temporal.
     */
    private static final int TOP_HOURS_LIMIT = 3;

    /**
     * Limite de dias da semana principais a serem considerados na análise temporal.
     */
    private static final int TOP_WEEKDAYS_LIMIT = 3;

    /**
     * Divisor para cálculo do índice da mediana.
     */
    private static final int MEDIAN_INDEX_DIVISOR = 2;

    /**
     * Divisor para cálculo do índice do primeiro quartil (Q1).
     */
    private static final int Q1_INDEX_DIVISOR = 4;

    /**
     * Multiplicador para cálculo do índice do terceiro quartil (Q3).
     */
    private static final int Q3_MULTIPLIER = 3;


    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param cardPatternRepository repositório para persistência de padrões de cartão
     * @param transactionRepository repositório para consulta de transações
     */
    public CardPatternService(CardPatternRepository cardPatternRepository,
                              TransactionRepository transactionRepository) {
        this.cardPatternRepository = cardPatternRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Constrói ou atualiza o padrão comportamental de um cartão com base em seu histórico de transações.
     * <p>
     * Este método realiza uma análise completa das transações do cartão, incluindo:
     * <ul>
     *   <li>Análise estatística de valores (média, mediana, desvio padrão, quartis)</li>
     *   <li>Análise de categorias de comerciantes</li>
     *   <li>Padrões temporais (horários e dias preferidos)</li>
     *   <li>Análise geográfica e de comerciantes</li>
     *   <li>Métricas comportamentais para detecção de anomalias</li>
     * </ul>
     * <p>
     * O resultado é armazenado em cache para otimização de performance.
     *
     * @param card o cartão para o qual o padrão será construído
     * @return o padrão comportamental construído ou atualizado
     * @throws RuntimeException se ocorrer erro durante a construção do padrão
     */
    @Cacheable(value = "cardPatterns", key = "#card.cardId")
    public CardPattern buildOrUpdateCardPattern(Card card) {
        log.debug("Building pattern for card: {}", card.getCardId());

        List<Transaction> transactions = transactionRepository.findByCard(card);
        log.info("Found {} transactions for card {}", transactions.size(), card.getCardId());

        if (transactions.isEmpty()) {
            log.warn("No transactions found for card {}", card.getCardId());
            return handleEmptyTransactions(card);
        }

        // Validação básica
        validateTransactions(transactions);

        CardPattern pattern = cardPatternRepository.findByCard(card)
                .orElseGet(() -> createNewPattern(card));

        try {
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

            CardPattern savedPattern = cardPatternRepository.save(pattern);
            log.info("Pattern successfully updated for card {}", card.getCardId());

            return savedPattern;

        } catch (Exception e) {
            log.error("Error building pattern for card {}: {}", card.getCardId(), e.getMessage(), e);
            throw new RuntimeException("Failed to build card pattern", e);
        }
    }

    /**
     * Invalida o padrão em cache para um cartão específico.
     * <p>
     * Este método deve ser chamado quando houver mudanças significativas no comportamento
     * do cartão que exijam reconstrução do padrão.
     *
     * @param card o cartão cujo padrão em cache será invalidado
     */
    @CacheEvict(value = "cardPatterns", key = "#card.cardId")
    public void invalidatePattern(Card card) {
        log.debug("Invalidating pattern cache for card: {}", card.getCardId());
    }

    /**
     * Cria um padrão vazio para cartões sem histórico de transações.
     * <p>
     * Utilizado quando um cartão novo é criado ou quando não há transações disponíveis
     * para análise. O padrão vazio será populado conforme transações forem realizadas.
     *
     * @param card o cartão para o qual criar um padrão vazio
     * @return o padrão vazio criado ou recuperado do banco de dados
     */
    private CardPattern handleEmptyTransactions(Card card) {
        return cardPatternRepository.findByCard(card)
                .orElseGet(() -> {
                    CardPattern pattern = createNewPattern(card);
                    return cardPatternRepository.save(pattern);
                });
    }

    /**
     * Cria uma nova instância de CardPattern associada a um cartão.
     *
     * @param card o cartão a ser associado ao novo padrão
     * @return uma nova instância de CardPattern
     */
    private CardPattern createNewPattern(Card card) {
        CardPattern pattern = new CardPattern();
        pattern.setCard(card);
        return pattern;
    }

    /**
     * Valida a integridade dos dados das transações antes do processamento.
     * <p>
     * Verifica a presença de valores nulos em campos críticos como valor da transação
     * e data/hora. Registra warnings no log para transações com dados inválidos.
     *
     * @param transactions lista de transações a serem validadas
     */
    private void validateTransactions(List<Transaction> transactions) {
        long nullAmounts = transactions.stream()
                .filter(t -> t.getAmount() == null)
                .count();

        long nullDates = transactions.stream()
                .filter(t -> t.getTransactionDateAndTime() == null)
                .count();

        if (nullAmounts > 0) {
            log.warn("Found {} transactions with null amount", nullAmounts);
        }

        if (nullDates > 0) {
            log.warn("Found {} transactions with null date", nullDates);
        }
    }

    /**
     * Realiza análise estatística completa dos valores das transações.
     * <p>
     * Calcula métricas estatísticas essenciais incluindo:
     * <ul>
     *   <li>Média aritmética dos valores</li>
     *   <li>Mediana (valor central)</li>
     *   <li>Desvio padrão (variabilidade)</li>
     *   <li>Quartis (Q1, Q3) e IQR (Intervalo Interquartil)</li>
     *   <li>Percentil 95 (threshold para valores atípicos)</li>
     *   <li>Valor máximo transacionado</li>
     *   <li>Distribuição de tickets (micro, pequeno, médio, grande)</li>
     * </ul>
     * <p>
     * Estas métricas são fundamentais para detecção de transações anômalas.
     *
     * @param pattern o padrão a ser atualizado com as métricas calculadas
     * @param transactions lista de transações para análise
     */
    private void analyzeTransactionAmounts(CardPattern pattern, List<Transaction> transactions) {
        List<BigDecimal> amounts = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(Transaction::getAmount)
                .sorted()
                .toList();

        if (amounts.isEmpty()) {
            log.warn("No valid amounts found for pattern analysis");
            return;
        }

        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
        pattern.setAvgTransactionAmount(avg);
        pattern.setMaxTransactionAmount(amounts.get(amounts.size() - 1));

        // ===== Evitar IndexOutOfBounds =====
        int medianIndex = Math.min(amounts.size() / MEDIAN_INDEX_DIVISOR, amounts.size() - 1);
        int q1Index = Math.min(amounts.size() / Q1_INDEX_DIVISOR, amounts.size() - 1);
        int q3Index = Math.min(Q3_MULTIPLIER * amounts.size() / Q1_INDEX_DIVISOR, amounts.size() - 1);

        BigDecimal median = amounts.get(medianIndex);
        BigDecimal q1 = amounts.get(q1Index);
        BigDecimal q3 = amounts.get(q3Index);

        BigDecimal variance = amounts.stream()
                .map(a -> a.subtract(avg).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        int p95Index = Math.min((int)Math.ceil(amounts.size() * PERCENTILE_95_THRESHOLD) - 1, amounts.size() - 1);
        BigDecimal percentile95 = amounts.get(p95Index);

        // Classificação de tickets
        Map<String, Long> ticketClassification = classifyTickets(amounts, q1, median, q3);
        if (ticketClassification == null) ticketClassification = new HashMap<>();

        pattern.setMedianAmount(median);
        pattern.setStdDevAmount(stdDev);
        pattern.setPercentile95Amount(percentile95);
        pattern.setQ1Amount(q1);
        pattern.setQ3Amount(q3);
        pattern.setIqrAmount(q3.subtract(q1));


        log.debug("Amount analysis - Avg: {}, Median: {}, StdDev: {}, P95: {}",
                avg, median, stdDev, percentile95);

    }

    /**
     * Classifica as transações por tamanho de ticket baseado em quartis.
     * <p>
     * Categorização:
     * <ul>
     *   <li><b>Micro:</b> valores abaixo do primeiro quartil (Q1)</li>
     *   <li><b>Pequeno:</b> valores entre Q1 e a mediana</li>
     *   <li><b>Médio:</b> valores entre a mediana e o terceiro quartil (Q3)</li>
     *   <li><b>Grande:</b> valores acima do terceiro quartil (Q3)</li>
     * </ul>
     *
     * @param amounts lista ordenada de valores das transações
     * @param q1 primeiro quartil (25º percentil)
     * @param median mediana (50º percentil)
     * @param q3 terceiro quartil (75º percentil)
     * @return mapa com a contagem de transações em cada categoria
     */
    private Map<String, Long> classifyTickets(List<BigDecimal> amounts,
                                              BigDecimal q1,
                                              BigDecimal median,
                                              BigDecimal q3) {
        return amounts.stream()
                .collect(Collectors.groupingBy(
                        amount -> {
                            if (amount.compareTo(q1) < 0) return "micro";
                            if (amount.compareTo(median) < 0) return "pequeno";
                            if (amount.compareTo(q3) < 0) return "medio";
                            return "grande";
                        },
                        Collectors.counting()
                ));
    }

    /**
     * Analisa os padrões de categorias de comerciantes das transações.
     * <p>
     * Identifica:
     * <ul>
     *   <li>Categorias mais frequentes (top 5)</li>
     *   <li>Distribuição de gastos por categoria</li>
     *   <li>Gasto médio por categoria</li>
     *   <li>Índice de diversificação (entropia) das categorias</li>
     * </ul>
     * <p>
     * A análise de categorias é útil para identificar mudanças de comportamento
     * e transações em categorias incomuns para o perfil do usuário.
     *
     * @param pattern o padrão a ser atualizado com as análises de categoria
     * @param transactions lista de transações para análise
     */
    private void analyzeCategories(CardPattern pattern, List<Transaction> transactions) {
        // Categorias mais frequentes com seus percentuais
        Map<MerchantCategory, Long> categoryFrequency = transactions.stream()
                .filter(t -> t.getMerchantCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getMerchantCategory,
                        Collectors.counting()
                ));

        if (categoryFrequency.isEmpty()) {
            log.warn("No valid merchant categories found");
            return;
        }

        List<MerchantCategory> topCategories = categoryFrequency.entrySet().stream()
                .sorted(Map.Entry.<MerchantCategory, Long>comparingByValue().reversed())
                .limit(TOP_CATEGORIES_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        pattern.setCommonCategories(topCategories);

        // Análise de gasto por categoria
        Map<MerchantCategory, BigDecimal> spendingByCategory = transactions.stream()
                .filter(t -> t.getMerchantCategory() != null && t.getAmount() != null)
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
                        e -> {
                            BigDecimal totalSpending = spendingByCategory.getOrDefault(
                                    e.getKey(),
                                    BigDecimal.ZERO
                            );
                            return totalSpending.divide(
                                    BigDecimal.valueOf(e.getValue()),
                                    2,
                                    RoundingMode.HALF_UP
                            );
                        }
                ));

        // Diversificação de categorias (índice de entropia)
        double categoryEntropy = calculateEntropy(
                categoryFrequency.values().stream()
                        .map(Long::doubleValue)
                        .toList()
        );

        log.debug("Category analysis - Top categories: {}, Entropy: {}",
                topCategories, categoryEntropy);

        // TODO: Adicionar esses campos na entidade CardPattern se necessário:
        // pattern.setCategoryDiversity(categoryEntropy);
        // pattern.setSpendingByCategory(spendingByCategory);
        // pattern.setAvgSpendingByCategory(avgByCategory);
    }

    /**
     * Realiza análise temporal sofisticada dos padrões de transação.
     * <p>
     * Analisa:
     * <ul>
     *   <li>Distribuição por hora do dia (24 buckets)</li>
     *   <li>Horários preferidos (top 3)</li>
     *   <li>Distribuição por dia da semana</li>
     *   <li>Dias da semana preferidos (top 3)</li>
     *   <li>Padrão weekend vs weekday</li>
     *   <li>Frequência média de transações por dia</li>
     *   <li>Intervalos médios entre transações consecutivas</li>
     * </ul>
     * <p>
     * A análise temporal é crítica para detectar transações realizadas em horários
     * atípicos ou com frequência incomum.
     *
     * @param pattern o padrão a ser atualizado com as análises temporais
     * @param transactions lista de transações para análise
     */
    private void analyzeTemporalPatterns(CardPattern pattern, List<Transaction> transactions) {
        List<Transaction> validTransactions = transactions.stream()
                .filter(t -> t.getTransactionDateAndTime() != null)
                .toList();

        if (validTransactions.isEmpty()) {
            log.warn("No valid transaction dates found");
            return;
        }

        // Análise por hora do dia (distribuição em 24 buckets)
        Map<Integer, Long> hourlyDistribution = validTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().getHour(),
                        Collectors.counting()
                ));

        // Top 3 horários mais frequentes
        List<Integer> topHours = hourlyDistribution.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(TOP_HOURS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        // Converte para LocalDateTime
        List<LocalDateTime> preferredHours = topHours.stream()
                .map(hour -> LocalDateTime.now()
                        .withHour(hour)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0))
                .toList();

        pattern.setPreferredHours(preferredHours);

        // Análise por dia da semana
        Map<DayOfWeek, Long> weekdayDistribution = validTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().getDayOfWeek(),
                        Collectors.counting()
                ));

        // Dias da semana preferidos
        List<DayOfWeek> preferredDays = weekdayDistribution.entrySet().stream()
                .sorted(Map.Entry.<DayOfWeek, Long>comparingByValue().reversed())
                .limit(TOP_WEEKDAYS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        // Padrão weekend vs weekday
        long weekendTxns = validTransactions.stream()
                .filter(t -> {
                    DayOfWeek day = t.getTransactionDateAndTime().getDayOfWeek();
                    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                })
                .count();

        double weekendRatio = validTransactions.isEmpty() ? 0.0 :
                (double) weekendTxns / validTransactions.size();

        // Frequência de transações por dia
        Map<String, Long> dailyFrequency = validTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDateAndTime().toLocalDate().toString(),
                        Collectors.counting()
                ));

        int avgDailyFrequency = dailyFrequency.isEmpty() ? 0 :
                (int) Math.round(
                        dailyFrequency.values().stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0)
                );

        pattern.setTransactionFrequencyPerDay(avgDailyFrequency);

        // Análise de intervalos entre transações
        List<Long> intervalsBetweenTransactions = calculateIntervals(validTransactions);
        double avgInterval = intervalsBetweenTransactions.isEmpty() ? 0.0 :
                intervalsBetweenTransactions.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);

        log.debug("Temporal analysis - Preferred hours: {}, Weekend ratio: {}, Avg interval: {} min",
                topHours, weekendRatio, avgInterval);

        // TODO: Adicionar esses campos na entidade CardPattern se necessário:
        // pattern.setPreferredWeekdays(preferredDays);
        // pattern.setWeekendTransactionRatio(weekendRatio);
        // pattern.setAvgIntervalBetweenTransactions(avgInterval); // em minutos
        // pattern.setHourlyDistribution(hourlyDistribution);
    }

    /**
     * Calcula os intervalos de tempo entre transações consecutivas.
     * <p>
     * Os intervalos são calculados em minutos e ordenados cronologicamente.
     * Esta métrica é útil para detectar padrões de velocidade de transação
     * e identificar rajadas de transações anômalas.
     *
     * @param transactions lista de transações válidas com datas
     * @return lista de intervalos em minutos entre transações consecutivas
     */
    private List<Long> calculateIntervals(List<Transaction> transactions) {
        if (transactions.size() < 2) {
            return Collections.emptyList();
        }

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
     * Analisa os padrões de comerciantes e localizações geográficas.
     * <p>
     * Identifica:
     * <ul>
     *   <li>Comerciantes mais frequentes (top 10)</li>
     *   <li>Diversidade de localizações únicas</li>
     *   <li>Distribuição geográfica das transações</li>
     * </ul>
     * <p>
     * A análise geográfica ajuda a detectar transações em locais incomuns
     * ou mudanças súbitas no padrão de localização.
     *
     * @param pattern o padrão a ser atualizado com as análises de localização
     * @param transactions lista de transações para análise
     */
    private void analyzeMerchantsAndLocations(CardPattern pattern, List<Transaction> transactions) {
        // Top comerciantes por frequência (usando categoria como proxy)
        Map<String, Long> merchantFrequency = transactions.stream()
                .map(t -> Optional.ofNullable(t.getMerchantCategory())
                        .map(Object::toString)
                        .orElse("UNKNOWN"))
                .collect(Collectors.groupingBy(
                        category -> category,
                        Collectors.counting()
                ));

        List<String> topMerchants = merchantFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_MERCHANTS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();

        // Análise de localização
        Set<String> uniqueLocations = transactions.stream()
                .map(t -> Optional.ofNullable(t.getCity())
                        .orElse("UNKNOWN"))
                .collect(Collectors.toSet());

        int locationDiversity = uniqueLocations.size();

        log.debug("Location analysis - Unique locations: {}, Top merchants: {}",
                locationDiversity, topMerchants.size());

        // TODO: Adicionar esses campos na entidade CardPattern se necessário:
        // pattern.setTopMerchants(topMerchants);
        // pattern.setLocationDiversity(locationDiversity);
        // pattern.setUniqueLocations(uniqueLocations);
    }

    /**
     * Calcula métricas comportamentais avançadas para detecção de anomalias.
     * <p>
     * Analisa:
     * <ul>
     *   <li>Velocidade de transações (máximo de transações por hora)</li>
     *   <li>Consistência temporal (desvio padrão dos horários de transação)</li>
     *   <li>Padrões de atividade em períodos específicos</li>
     * </ul>
     * <p>
     * Estas métricas são essenciais para identificar comportamentos anômalos como:
     * <ul>
     *   <li>Rajadas de transações em curto período</li>
     *   <li>Mudanças súbitas no padrão temporal</li>
     *   <li>Atividades fora do perfil normal do usuário</li>
     * </ul>
     *
     * @param pattern o padrão a ser atualizado com as métricas comportamentais
     * @param transactions lista de transações para análise
     */
    private void analyzeBehavioralMetrics(CardPattern pattern, List<Transaction> transactions) {
        List<Transaction> validTransactions = transactions.stream()
                .filter(t -> t.getTransactionDateAndTime() != null)
                .toList();

        if (validTransactions.isEmpty()) {
            return;
        }

        // Velocidade de transações (transações por hora em períodos ativos)
        Map<String, Long> hourlyActivity = validTransactions.stream()
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
        List<Integer> hours = validTransactions.stream()
                .map(t -> t.getTransactionDateAndTime().getHour())
                .toList();

        double avgHour = hours.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(12.0);

        double hourStdDev = Math.sqrt(
                hours.stream()
                        .mapToDouble(h -> Math.pow(h - avgHour, 2))
                        .average()
                        .orElse(0)
        );

        log.debug("Behavioral metrics - Max txns/hour: {}, Temporal consistency: {}",
                maxTransactionsPerHour, hourStdDev);

        // TODO: Adicionar esses campos na entidade CardPattern se necessário:
        // pattern.setMaxTransactionsPerHour(maxTransactionsPerHour);
        // pattern.setTemporalConsistency(hourStdDev); // menor = mais consistente
    }

    /**
     * Calcula a entropia (índice de Shannon) de uma distribuição.
     * <p>
     * A entropia é uma medida de diversidade ou aleatoriedade em uma distribuição:
     * <ul>
     *   <li><b>Entropia próxima a 0:</b> comportamento concentrado/previsível</li>
     *   <li><b>Entropia alta:</b> comportamento diversificado/distribuído</li>
     * </ul>
     * <p>
     * Fórmula: H(X) = -Σ(p(x) * log₂(p(x)))
     * <p>
     * Utilizada para medir a diversificação de categorias, comerciantes ou outros padrões.
     *
     * @param distribution lista de frequências ou contagens
     * @return valor da entropia (0 a log₂(n), onde n é o número de elementos)
     */
    private double calculateEntropy(List<Double> distribution) {
        if (distribution.isEmpty()) {
            return 0.0;
        }

        double total = distribution.stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (total == 0.0) {
            return 0.0;
        }

        return distribution.stream()
                .mapToDouble(count -> {
                    if (count == 0) return 0;
                    double p = count / total;
                    return -p * (Math.log(p) / Math.log(2));
                })
                .sum();
    }
}