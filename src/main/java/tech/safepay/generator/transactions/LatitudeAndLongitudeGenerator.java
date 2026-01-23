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
    private static final DecimalFormat FORMAT = new DecimalFormat("#.######");

    // Tentativas máximas para encontrar terra firme
    private static final int MAX_ATTEMPTS = 30;

    private final TransactionRepository transactionRepository;

    public LatitudeAndLongitudeGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public String[] generateLocation(Card card) {
        var lastTransactions = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        // Fallback seguro: sem histórico ou dados inválidos
        if (lastTransactions.isEmpty()
                || lastTransactions.get(0).getLatitude() == null
                || lastTransactions.get(0).getLongitude() == null) {
            return generateRandomLocationOnLand();
        }

        // Usa a última transação como ponto base
        Transaction last = lastTransactions.get(0);

        double baseLat = Double.parseDouble(last.getLatitude());
        double baseLon = Double.parseDouble(last.getLongitude());

        // IMPORTANTE: Valida se o ponto base está em terra firme
        // Se não estiver, gera uma nova localização válida
        if (!isOnLand(baseLat, baseLon)) {
            return generateRandomLocationOnLand();
        }

        boolean anomaly = RANDOM.nextDouble() < ANOMALY_PROBABILITY;
        double radius = anomaly ? ANOMALY_RADIUS_KM : NORMAL_RADIUS_KM;

        // Tenta gerar uma coordenada em terra firme
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double[] point = generatePointWithinRadius(baseLat, baseLon, radius);

            if (isOnLand(point[0], point[1])) {
                return new String[]{
                        FORMAT.format(point[0]),
                        FORMAT.format(point[1])
                };
            }
        }

        // Se todas as tentativas falharem, usa o ponto base original (já validado)
        return new String[]{
                FORMAT.format(baseLat),
                FORMAT.format(baseLon)
        };
    }

    // Geração baseada em um ponto + raio
    private double[] generatePointWithinRadius(double baseLat, double baseLon, double radiusKm) {
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

    // Fallback: localização aleatória em terra firme (mundo todo)
    private String[] generateRandomLocationOnLand() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Gera coordenadas aleatórias no mundo todo
            double lat = -90 + RANDOM.nextDouble() * 180;  // -90 até +90
            double lon = -180 + RANDOM.nextDouble() * 360; // -180 até +180

            if (isOnLand(lat, lon)) {
                return new String[]{
                        FORMAT.format(lat),
                        FORMAT.format(lon)
                };
            }
        }

        // Fallback final: Nova York (garantido em terra)
        return new String[]{"40.712776", "-74.005974"};
    }

    /**
     * Verifica se as coordenadas estão em terra firme (qualquer continente).
     *
     * Cobertura aproximada de todos os continentes habitados:
     * - América do Norte e Central
     * - América do Sul
     * - Europa
     * - África
     * - Ásia
     * - Oceania
     *
     * EXCLUÍDOS: Ártico, Antártica, regiões extremas sem cobertura geocoding.
     *
     * Para precisão total em produção, use GeoTools + shapefile mundial.
     */
    private boolean isOnLand(double lat, double lon) {
        // Validação básica de limites globais
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return false;
        }

        // EXCLUIR REGIÕES EXTREMAS (Ártico e Antártica)
        // APIs de geocoding têm baixa cobertura nessas áreas
        if (lat > 70.0 || lat < -60.0) {
            return false;
        }

        // AMÉRICA DO NORTE
        if (isInNorthAmerica(lat, lon)) return true;

        // AMÉRICA CENTRAL E CARIBE
        if (isInCentralAmerica(lat, lon)) return true;

        // AMÉRICA DO SUL
        if (isInSouthAmerica(lat, lon)) return true;

        // EUROPA
        if (isInEurope(lat, lon)) return true;

        // ÁFRICA
        if (isInAfrica(lat, lon)) return true;

        // ÁSIA
        if (isInAsia(lat, lon)) return true;

        // OCEANIA
        if (isInOceania(lat, lon)) return true;

        return false;
    }

    // América do Norte (Canadá, EUA, México)
    private boolean isInNorthAmerica(double lat, double lon) {
        // Canadá (LIMITADO até latitude 70° para evitar Ártico)
        if (lat >= 41.0 && lat <= 70.0 && lon >= -141.0 && lon <= -52.0) {
            // Exclui baía de Hudson
            if (lat >= 55.0 && lat <= 65.0 && lon >= -95.0 && lon <= -77.0) return false;
            // Exclui arquipélago ártico (acima de 70°)
            if (lat > 70.0) return false;
            return true;
        }

        // EUA Continental
        if (lat >= 24.0 && lat <= 49.0 && lon >= -125.0 && lon <= -66.0) {
            // Exclui Grandes Lagos parcialmente
            if (lat >= 41.0 && lat <= 49.0 && lon >= -92.0 && lon <= -76.0) {
                if (lat >= 43.0 && lon >= -83.0 && lon <= -76.0) return false;
            }
            return true;
        }

        // Alasca
        if (lat >= 51.0 && lat <= 71.0 && lon >= -169.0 && lon <= -130.0) return true;

        // México
        if (lat >= 14.5 && lat <= 32.7 && lon >= -118.0 && lon <= -86.0) return true;

        return false;
    }

    // América Central e Caribe
    private boolean isInCentralAmerica(double lat, double lon) {
        if (lat >= 7.0 && lat <= 18.0 && lon >= -92.0 && lon <= -77.0) return true;

        // Caribe (Cuba, Jamaica, República Dominicana, etc)
        if (lat >= 18.0 && lat <= 23.0 && lon >= -85.0 && lon <= -71.0) return true;

        return false;
    }

    // América do Sul
    private boolean isInSouthAmerica(double lat, double lon) {
        // Brasil
        if (lat >= -33.75 && lat <= 5.3 && lon >= -74.0 && lon <= -34.5) {
            // Exclui costa Atlântica onde apropriado
            if (lon >= -39.0 && lat >= -13.0 && lat <= -2.5) return false;
            return true;
        }

        // Argentina e Chile
        if (lat >= -55.0 && lat <= -21.0 && lon >= -75.0 && lon <= -53.0) return true;

        // Colômbia e Venezuela
        if (lat >= -4.0 && lat <= 12.5 && lon >= -79.0 && lon <= -59.0) return true;

        // Peru, Bolívia, Paraguai
        if (lat >= -23.0 && lat <= -0.0 && lon >= -81.0 && lon <= -57.0) return true;

        // Uruguai
        if (lat >= -35.0 && lat <= -30.0 && lon >= -58.5 && lon <= -53.0) return true;

        return false;
    }

    // Europa
    private boolean isInEurope(double lat, double lon) {
        // Europa Ocidental (França, Espanha, Portugal, UK, Irlanda)
        if (lat >= 36.0 && lat <= 60.0 && lon >= -10.0 && lon <= 10.0) return true;

        // Europa Central (Alemanha, Polônia, Rep. Tcheca, Áustria, Suíça)
        if (lat >= 45.0 && lat <= 55.0 && lon >= 5.0 && lon <= 25.0) return true;

        // Europa do Norte (Escandinávia - LIMITADA até 70°)
        if (lat >= 55.0 && lat <= 70.0 && lon >= 4.0 && lon <= 31.0) return true;

        // Europa do Sul (Itália, Grécia, Balcãs)
        if (lat >= 36.0 && lat <= 47.0 && lon >= 6.0 && lon <= 28.0) return true;

        // Europa Oriental (Rússia europeia, Ucrânia, Bielorrússia)
        if (lat >= 44.0 && lat <= 70.0 && lon >= 20.0 && lon <= 60.0) return true;

        return false;
    }

    // África
    private boolean isInAfrica(double lat, double lon) {
        // Norte da África (Egito, Líbia, Argélia, Marrocos, Tunísia)
        if (lat >= 15.0 && lat <= 37.0 && lon >= -17.0 && lon <= 37.0) return true;

        // África Ocidental (Nigéria, Gana, Senegal, etc)
        if (lat >= 4.0 && lat <= 20.0 && lon >= -17.0 && lon <= 15.0) return true;

        // África Central (Congo, Camarões, etc)
        if (lat >= -13.0 && lat <= 8.0 && lon >= 7.0 && lon <= 32.0) return true;

        // África Oriental (Quênia, Tanzânia, Etiópia, etc)
        if (lat >= -12.0 && lat <= 18.0 && lon >= 22.0 && lon <= 51.0) return true;

        // África Austral (África do Sul, Angola, Moçambique, etc)
        if (lat >= -35.0 && lat <= -15.0 && lon >= 11.0 && lon <= 41.0) return true;

        // Madagascar
        if (lat >= -25.0 && lat <= -12.0 && lon >= 43.0 && lon <= 51.0) return true;

        return false;
    }

    // Ásia
    private boolean isInAsia(double lat, double lon) {
        // Rússia Asiática (Sibéria - LIMITADA até 70°)
        if (lat >= 50.0 && lat <= 70.0 && lon >= 60.0 && lon <= 180.0) return true;

        // Ásia Central (Cazaquistão, Uzbequistão, etc)
        if (lat >= 35.0 && lat <= 55.0 && lon >= 46.0 && lon <= 87.0) return true;

        // Oriente Médio (Arábia Saudita, Irã, Iraque, Turquia, etc)
        if (lat >= 12.0 && lat <= 43.0 && lon >= 26.0 && lon <= 63.0) return true;

        // Sul da Ásia (Índia, Paquistão, Bangladesh, etc)
        if (lat >= 6.0 && lat <= 37.0 && lon >= 60.0 && lon <= 97.0) return true;

        // Sudeste Asiático (Tailândia, Vietnã, Malásia, Indonésia, etc)
        if (lat >= -11.0 && lat <= 28.0 && lon >= 92.0 && lon <= 141.0) return true;

        // Leste Asiático (China, Mongólia)
        if (lat >= 18.0 && lat <= 53.0 && lon >= 73.0 && lon <= 135.0) return true;

        // Japão
        if (lat >= 24.0 && lat <= 46.0 && lon >= 123.0 && lon <= 146.0) return true;

        // Coreia
        if (lat >= 33.0 && lat <= 43.0 && lon >= 124.0 && lon <= 132.0) return true;

        // Filipinas
        if (lat >= 5.0 && lat <= 21.0 && lon >= 117.0 && lon <= 127.0) return true;

        return false;
    }

    // Oceania
    private boolean isInOceania(double lat, double lon) {
        // Austrália
        if (lat >= -44.0 && lat <= -10.0 && lon >= 113.0 && lon <= 154.0) return true;

        // Nova Zelândia
        if (lat >= -47.0 && lat <= -34.0 && lon >= 166.0 && lon <= 179.0) return true;

        // Papua Nova Guiné
        if (lat >= -11.0 && lat <= -1.0 && lon >= 140.0 && lon <= 156.0) return true;

        // Ilhas do Pacífico (Fiji, Samoa, Tonga, etc)
        if (lat >= -21.0 && lat <= -13.0 && lon >= 177.0 && lon <= -169.0) return true;

        return false;
    }
}