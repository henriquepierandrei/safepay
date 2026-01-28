package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Componente responsável por validações baseadas em geolocalização de transações.
 * <p>
 * Esta classe implementa mecanismos de detecção de anomalias relacionadas à localização
 * geográfica das transações, identificando padrões suspeitos como uso em países de alto
 * risco, mudanças bruscas de localização e viagens fisicamente impossíveis.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de transações em países de alto risco (HIGH_RISK_COUNTRY)</li>
 *   <li>Detecção de anomalias de localização (LOCATION_ANOMALY)</li>
 *   <li>Detecção de viagens impossíveis (IMPOSSIBLE_TRAVEL)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class LocationValidation {

    /**
     * Serviço de cache para resolução de coordenadas geográficas em códigos de país.
     */
    private final GeoLocationCacheService geoLocationCacheService;

    /**
     * Conjunto de códigos de países considerados de alto risco para fraude.
     * <p>
     * Códigos no formato ISO 3166-1 alpha-2:
     * <ul>
     *   <li>RU - Rússia</li>
     *   <li>NG - Nigéria</li>
     *   <li>IR - Irã</li>
     *   <li>KP - Coreia do Norte</li>
     *   <li>UA - Ucrânia</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Nota:</strong> Em ambiente de produção, esta lista deve ser mantida
     * em configuração externa ou através de serviço dedicado para permitir atualizações
     * dinâmicas sem necessidade de deploy.
     * </p>
     */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "RU", "NG", "IR", "KP", "UA"
    );

    /**
     * Raio da Terra em quilômetros, utilizado no cálculo da distância de Haversine.
     */
    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Distância mínima em km para considerar anomalia de localização.
     */
    private static final double LOCATION_ANOMALY_THRESHOLD_KM = 300;

    /**
     * Distância mínima em km para considerar viagem impossível.
     */
    private static final double IMPOSSIBLE_TRAVEL_DISTANCE_KM = 1000;

    /**
     * Tempo máximo em horas para considerar viagem impossível.
     */
    private static final double IMPOSSIBLE_TRAVEL_TIME_HOURS = 1;

    /**
     * Construtor para injeção de dependências.
     *
     * @param geoLocationCacheService serviço de cache para resolução de geolocalização
     */
    public LocationValidation(GeoLocationCacheService geoLocationCacheService) {
        this.geoLocationCacheService = geoLocationCacheService;
    }

    /**
     * Valida se a transação originou-se de um país considerado de alto risco.
     * <p>
     * Esta validação identifica transações realizadas em países conhecidos por
     * apresentarem índices elevados de fraude ou atividades suspeitas, baseando-se
     * em lista configurada de códigos de país.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Resolve as coordenadas da transação para código de país (ISO 3166-1 alpha-2)</li>
     *   <li>Verifica se o código está presente na lista de países de alto risco</li>
     *   <li>Sinaliza quando há correspondência</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> definido em {@link AlertType#HIGH_RISK_COUNTRY}
     * </p>
     *
     * @param transaction a transação sendo validada, contendo latitude e longitude
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se não for possível resolver o país ou se o país
     *         não estiver na lista de alto risco.
     * @see AlertType#HIGH_RISK_COUNTRY
     * @see GeoLocationCacheService#resolveCountry(String, String)
     */
    public ValidationResultDto highRiskCountryValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        String countryCode = geoLocationCacheService
                .resolveCountry(
                        transaction.getLatitude(),
                        transaction.getLongitude()
                );

        if (countryCode == null) return result;

        if (HIGH_RISK_COUNTRIES.contains(countryCode.toUpperCase())) {
            result.addScore(AlertType.HIGH_RISK_COUNTRY.getScore());
            result.addAlert(AlertType.HIGH_RISK_COUNTRY);
        }

        return result;
    }

    /**
     * Valida se há anomalia na localização comparada com o histórico recente.
     * <p>
     * Esta validação detecta mudanças significativas de localização geográfica
     * entre a transação atual e a mais recente do histórico, indicando possível
     * uso do cartão em localização atípica ou não autorizada.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera o histórico recente (últimas 20 transações)</li>
     *   <li>Identifica a transação mais recente anterior à atual</li>
     *   <li>Calcula a distância geográfica usando fórmula de Haversine</li>
     *   <li>Sinaliza se a distância exceder 300 km</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Critérios Mínimos:</strong>
     * <ul>
     *   <li>Mínimo de 2 transações no histórico</li>
     *   <li>Distância superior a 300 km da transação anterior</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> definido em {@link AlertType#LOCATION_ANOMALY}
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: não houver histórico suficiente, não houver
     *         transação anterior, ou a distância não exceder o threshold.
     * @see AlertType#LOCATION_ANOMALY
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     * @see #haversineDistance(double, double, double, double)
     */
    public ValidationResultDto locationAnomalyValidation(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> history = snapshot.last20();
        if (history == null || history.size() < 2) return result;

        Transaction reference = history.stream()
                .filter(t -> t.getCreatedAt().isBefore(transaction.getCreatedAt()))
                .max(Comparator.comparing(Transaction::getCreatedAt))
                .orElse(null);

        if (reference == null) return result;

        double distanceKm = haversineDistance(
                Double.parseDouble(reference.getLatitude()),
                Double.parseDouble(reference.getLongitude()),
                Double.parseDouble(transaction.getLatitude()),
                Double.parseDouble(transaction.getLongitude())
        );

        if (distanceKm > LOCATION_ANOMALY_THRESHOLD_KM) {
            result.addScore(AlertType.LOCATION_ANOMALY.getScore());
            result.addAlert(AlertType.LOCATION_ANOMALY);
        }

        return result;
    }

    /**
     * Valida se há viagem fisicamente impossível entre transações consecutivas.
     * <p>
     * Esta validação detecta casos onde a distância entre duas transações consecutivas
     * e o intervalo de tempo entre elas tornam fisicamente impossível que o mesmo
     * usuário tenha realizado ambas as transações, indicando forte probabilidade de
     * fraude ou clonagem de cartão.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera o histórico recente (últimas 20 transações)</li>
     *   <li>Identifica a transação imediatamente anterior</li>
     *   <li>Calcula distância geográfica e intervalo de tempo</li>
     *   <li>Calcula velocidade necessária para percorrer a distância no tempo disponível</li>
     *   <li>Sinaliza se a distância for superior a 1000 km em menos de 1 hora</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Regra de Ativação:</strong>
     * <br>
     * {@code distância > 1000 km AND tempo < 1 hora}
     * </p>
     * <p>
     * <strong>Critérios Mínimos:</strong>
     * <ul>
     *   <li>Mínimo de 2 transações no histórico</li>
     *   <li>Intervalo de tempo positivo entre transações</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> definido em {@link AlertType#IMPOSSIBLE_TRAVEL}
     * <br>
     * Sinal extremamente forte de fraude, frequentemente justifica bloqueio imediato.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: não houver histórico suficiente, não houver
     *         transação anterior, intervalo de tempo for inválido, ou não caracterizar
     *         viagem impossível.
     * @see AlertType#IMPOSSIBLE_TRAVEL
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     * @see #haversineDistance(double, double, double, double)
     */
    public ValidationResultDto impossibleTravelValidation(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> history = snapshot.last20();
        if (history == null || history.size() < 2) return result;

        // previous = última transação já registrada
        Transaction previous = history.stream()
                .filter(t -> t.getCreatedAt().isBefore(transaction.getCreatedAt()))
                .max(Comparator.comparing(Transaction::getCreatedAt))
                .orElse(null);

        if (previous == null) return result;

        double distanceKm = haversineDistance(
                Double.parseDouble(previous.getLatitude()),
                Double.parseDouble(previous.getLongitude()),
                Double.parseDouble(transaction.getLatitude()),
                Double.parseDouble(transaction.getLongitude())
        );

        long secondsDiff =
                Duration.between(previous.getCreatedAt(), transaction.getCreatedAt()).getSeconds();

        if (secondsDiff <= 0) return result;

        double hours = secondsDiff / 3600.0;
        double requiredSpeed = distanceKm / hours;

        if (distanceKm > IMPOSSIBLE_TRAVEL_DISTANCE_KM && hours < IMPOSSIBLE_TRAVEL_TIME_HOURS) {
            result.addScore(AlertType.IMPOSSIBLE_TRAVEL.getScore());
            result.addAlert(AlertType.IMPOSSIBLE_TRAVEL);
        }

        return result;
    }

    /**
     * Calcula a distância geográfica entre dois pontos usando a fórmula de Haversine.
     * <p>
     * A fórmula de Haversine determina a distância do grande círculo entre dois pontos
     * em uma esfera a partir de suas latitudes e longitudes, considerando a curvatura
     * da Terra. É amplamente utilizada em sistemas de navegação e geolocalização.
     * </p>
     * <p>
     * <strong>Fórmula:</strong>
     * <pre>
     * a = sin²(Δφ/2) + cos(φ1) × cos(φ2) × sin²(Δλ/2)
     * c = 2 × atan2(√a, √(1−a))
     * d = R × c
     * </pre>
     * onde φ é latitude, λ é longitude, R é o raio da Terra (6371 km)
     * </p>
     *
     * @param lat1 latitude do primeiro ponto em graus decimais
     * @param lon1 longitude do primeiro ponto em graus decimais
     * @param lat2 latitude do segundo ponto em graus decimais
     * @param lon2 longitude do segundo ponto em graus decimais
     * @return distância entre os dois pontos em quilômetros
     */
    private double haversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_KM * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}