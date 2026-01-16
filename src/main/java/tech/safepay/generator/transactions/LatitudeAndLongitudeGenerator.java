package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.text.DecimalFormat;
import java.util.Random;

@Component
public class LatitudeAndLongitudeGenerator {

    private static final Random RANDOM = new Random();

    // Probabilidade de anomalia geográfica
    private static final double ANOMALY_PROBABILITY = 0.05;

    // Raios em KM
    private static final double NORMAL_RADIUS_KM = 10;
    private static final double ANOMALY_RADIUS_KM = 1500;

    // Precisão realista
    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.######");

    private final TransactionRepository transactionRepository;

    public LatitudeAndLongitudeGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String[] generateLocation(Card card) {

        var lastTransactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        // Fallback seguro: sem histórico ou dados inválidos
        if (lastTransactions.isEmpty()
                || lastTransactions.get(0).getLatitude() == null
                || lastTransactions.get(0).getLongitude() == null) {

            return generateRandomLocation();
        }

        // Usa a última transação como ponto base
        Transaction last = lastTransactions.get(0);

        double baseLat = Double.parseDouble(last.getLatitude());
        double baseLon = Double.parseDouble(last.getLongitude());

        boolean anomaly = RANDOM.nextDouble() < ANOMALY_PROBABILITY;
        double radius = anomaly ? ANOMALY_RADIUS_KM : NORMAL_RADIUS_KM;

        double[] point = generatePointWithinRadius(baseLat, baseLon, radius);

        return new String[]{
                FORMAT.format(point[0]),
                FORMAT.format(point[1])
        };
    }

    // Geração baseada em um ponto + raio
    private double[] generatePointWithinRadius(
            double baseLat,
            double baseLon,
            double radiusKm
    ) {
        double radiusInDegrees = radiusKm / 111.0;

        double u = RANDOM.nextDouble();
        double v = RANDOM.nextDouble();

        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;

        double deltaLat = w * Math.cos(t);
        double deltaLon = w * Math.sin(t) / Math.cos(Math.toRadians(baseLat));

        return new double[]{
                baseLat + deltaLat,
                baseLon + deltaLon
        };
    }

    // Fallback: localização genérica (ex: Brasil)
    private String[] generateRandomLocation() {
        double lat = -33 + RANDOM.nextDouble() * 13; // -33 até -20
        double lon = -57 + RANDOM.nextDouble() * 13; // -57 até -44

        return new String[]{
                FORMAT.format(lat),
                FORMAT.format(lon)
        };
    }
}
