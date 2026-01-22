package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class LocationValidation {
    private final GeoLocationCacheService geoLocationCacheService;

    /**
     * Países com alto risco de fraude.
     * Em produção: config externa ou serviço dedicado.
     */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "RU", "NG", "IR", "KP", "UA"
    );

    public LocationValidation(
            GeoLocationCacheService geoLocationCacheService
    ) {
        this.geoLocationCacheService = geoLocationCacheService;
    }

    /**
     * =========================
     * HIGH_RISK_COUNTRY
     * =========================
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
     * =========================
     * LOCATION_ANOMALY
     * =========================
     */
    public ValidationResultDto locationAnomalyValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> history = snapshot.last20();
        if (history == null || history.size() < 5) return result;

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
     */
    public ValidationResultDto impossibleTravelValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> history = snapshot.last20();
        if (history == null || history.size() < 2) return result;

        // index 0 = atual, index 1 = última anterior
        Transaction previous = history.get(1);

        double distanceKm = haversineDistance(
                Double.parseDouble(previous.getLatitude()),
                Double.parseDouble(previous.getLongitude()),
                Double.parseDouble(transaction.getLatitude()),
                Double.parseDouble(transaction.getLongitude())
        );

        long minutesDiff =
                Duration.between(previous.getCreatedAt(), transaction.getCreatedAt()).toMinutes();

        if (minutesDiff <= 0) return result;

        double requiredSpeed = (distanceKm / minutesDiff) * 60;

        if (requiredSpeed > 900) {
            result.addScore(AlertType.IMPOSSIBLE_TRAVEL.getScore());
            result.addAlert(AlertType.IMPOSSIBLE_TRAVEL);
        }

        return result;
    }

    /**
     * Distância geográfica (Haversine)
     */
    private double haversineDistance(
            double lat1, double lon1,
            double lat2, double lon2
    ) {
        final int R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
