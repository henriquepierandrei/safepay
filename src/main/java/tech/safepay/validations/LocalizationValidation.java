package tech.safepay.validations;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tech.safepay.Enums.AlertType;
import tech.safepay.configs.ResolveLocalizationConfig;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class LocalizationValidation {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ResolveLocalizationConfig resolveLocalizationConfig;

    /**
     * Lista de países com alto índice de fraude.
     * Em produção, isso vem de config ou serviço externo.
     */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "RU", "NG", "IR", "KP", "UA"
    );

    public LocalizationValidation(TransactionRepository transactionRepository, ResolveLocalizationConfig resolveLocalizationConfig) {
        this.transactionRepository = transactionRepository;
        this.resolveLocalizationConfig = resolveLocalizationConfig;
    }

    /**
     * =========================
     * HIGH_RISK_COUNTRY
     * =========================
     *
     * Usa reverse geocoding (Nominatim / OpenStreetMap)
     * para identificar o país da transação e verificar
     * se ele está classificado como alto risco.
     */
    public ValidationResultDto highRiskCountryValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        String countryCode = resolveLocalizationConfig.resolve(transaction.getLatitude(), transaction.getLongitude()).countryCode();
        if (countryCode == null) return result;

        System.out.println(countryCode);

        if (HIGH_RISK_COUNTRIES.contains(countryCode.toUpperCase())) {
            result.addScore(AlertType.HIGH_RISK_COUNTRY.getScore());
            result.addAlert(AlertType.HIGH_RISK_COUNTRY);
        }

        return result;
    }

    /**
     * =========================
     * LOCATION_ANOMALY
     * =========================
     *
     * Detecta se a localização atual foge do padrão
     * histórico do cartão (desvio geográfico).
     *
     * Regra moderada, depende de contexto.
     */
    public ValidationResultDto locationAnomalyValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        List<Transaction> history = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);
        if (history.size() < 5) return result;

        double avgLat = history.stream()
                .mapToDouble(t -> Double.parseDouble(t.getLatitude()))
                .average().orElse(0);

        double avgLon = history.stream()
                .mapToDouble(t -> Double.parseDouble(t.getLongitude()))
                .average().orElse(0);

        double distanceKm = haversineDistance(
                avgLat,
                avgLon,
                Double.parseDouble(transaction.getLatitude()),
                Double.parseDouble(transaction.getLongitude())
        );

        if (distanceKm > 300) {
            result.addScore(AlertType.LOCATION_ANOMALY.getScore());
            result.addAlert(AlertType.LOCATION_ANOMALY);
        }

        return result;
    }

    /**
     * =========================
     * IMPOSSIBLE_TRAVEL
     * =========================
     *
     * Verifica se a distância entre a última transação
     * e a atual é incompatível com o tempo decorrido.
     *
     * Sinal forte, quase determinístico de fraude.
     */
    public ValidationResultDto impossibleTravelValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        Optional<Transaction> lastTransaction =
                transactionRepository
                        .findFirstByCardAndTransactionIdNotOrderByCreatedAtDesc(
                                card,
                                transaction.getTransactionId()
                        );

        if (lastTransaction.isEmpty()) return result;

        Transaction previous = lastTransaction.get();

        double distanceKm = haversineDistance(
                Double.parseDouble(previous.getLatitude()),
                Double.parseDouble(previous.getLongitude()),
                Double.parseDouble(transaction.getLatitude()),
                Double.parseDouble(transaction.getLongitude())
        );

        long minutesDiff = Duration.between(previous.getCreatedAt(), transaction.getCreatedAt()).toMinutes();
        if (minutesDiff <= 0) return result;

        double requiredSpeed = (distanceKm / minutesDiff) * 60;

        if (requiredSpeed > 900) {
            result.addScore(AlertType.IMPOSSIBLE_TRAVEL.getScore());
            result.addAlert(AlertType.IMPOSSIBLE_TRAVEL);
        }

        return result;
    }

    /**
     * Cálculo de distância geográfica (Haversine).
     */
    private double haversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        final int R = 6371; // raio da Terra em km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
