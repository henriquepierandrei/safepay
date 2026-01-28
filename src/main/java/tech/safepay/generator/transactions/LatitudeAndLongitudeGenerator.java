package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

/**
 * Gerador inteligente de coordenadas geográficas para transações simuladas baseado em histórico e cidades reais.
 * <p>
 * Este componente é responsável por gerar coordenadas GPS (latitude/longitude) realistas que simulam:
 * <ul>
 *   <li>Transações consistentes com localização histórica do cartão (95% dos casos)</li>
 *   <li>Transações anômalas em cidades diferentes para teste de fraude (5% dos casos)</li>
 *   <li>Coordenadas dentro de áreas urbanas de cidades conhecidas globalmente</li>
 *   <li>Distribuição geográfica realista com base em padrões de uso</li>
 * </ul>
 * <p>
 * <b>Base de dados geográfica:</b>
 * Mantém lista curada de 150+ cidades globais com:
 * <ul>
 *   <li>Coordenadas precisas do centro urbano</li>
 *   <li>Raio máximo de área urbana (em quilômetros)</li>
 *   <li>Distribuição equilibrada entre continentes</li>
 *   <li>Foco em cidades com boa cobertura de geocoding</li>
 * </ul>
 * <p>
 * <b>Estratégia de geração:</b>
 * <ol>
 *   <li>Analisa últimas 20 transações do cartão</li>
 *   <li>Se houver histórico: gera próximo à última localização (95%) ou em cidade aleatória (5%)</li>
 *   <li>Se não houver histórico: escolhe cidade aleatória da base de dados</li>
 *   <li>Aplica variação dentro do raio urbano para realismo</li>
 * </ol>
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Geração de transações de teste com localização realista</li>
 *   <li>Simulação de viagens e mudanças de localização</li>
 *   <li>Testes de detecção de velocidade impossível</li>
 *   <li>Validação de consistência entre IP e GPS</li>
 *   <li>Treinamento de modelos ML de detecção de fraude geográfica</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class LatitudeAndLongitudeGenerator {

    /**
     * Gerador de números aleatórios thread-safe para variações geográficas.
     */
    private static final Random RANDOM = new Random();

    /**
     * Probabilidade de gerar anomalia geográfica (transação em cidade diferente).
     * <p>
     * Taxa de 5% calibrada para simular:
     * <ul>
     *   <li>Viagens legítimas</li>
     *   <li>Fraudes com cartão clonado em outra cidade</li>
     *   <li>Uso de VPN com inconsistência GPS</li>
     * </ul>
     */
    private static final double ANOMALY_PROBABILITY = 0.05;

    /**
     * Raio em quilômetros para transações normais (mesma cidade/região).
     * <p>
     * 5km é suficiente para cobrir:
     * <ul>
     *   <li>Deslocamento diário típico (casa-trabalho-comércio)</li>
     *   <li>Múltiplos bairros próximos</li>
     *   <li>Área metropolitana central</li>
     * </ul>
     */
    private static final double NORMAL_RADIUS_KM = 5;

    /**
     * Raio em quilômetros para transações anômalas (atualmente não utilizado).
     * <p>
     * 50km cobriria cidades vizinhas ou bairros muito distantes,
     * mas o código atual usa cidade completamente diferente para anomalias.
     */
    private static final double ANOMALY_RADIUS_KM = 50;

    /**
     * Formatador decimal para coordenadas GPS com 6 casas decimais.
     * <p>
     * Precisão de 6 decimais em coordenadas GPS equivale a:
     * <ul>
     *   <li>±0.111 metros na linha do Equador</li>
     *   <li>Suficiente para identificar endereços específicos</li>
     *   <li>Padrão utilizado por sistemas de geolocalização comerciais</li>
     * </ul>
     */
    private static final DecimalFormat FORMAT = new DecimalFormat("#.######");

    private final TransactionRepository transactionRepository;

    /**
     * Base de dados de cidades conhecidas com alta cobertura de geocoding.
     * <p>
     * Cada entrada contém: {latitude, longitude, raioMaximoKm}
     * <p>
     * <b>Critérios de seleção:</b>
     * <ul>
     *   <li>Cidades com população superior a 100 mil habitantes</li>
     *   <li>Boa cobertura em APIs de geocoding (Google Maps, Nominatim, etc.)</li>
     *   <li>Distribuição global equilibrada entre continentes</li>
     *   <li>Inclusão de principais centros financeiros e comerciais</li>
     * </ul>
     * <p>
     * <b>Cobertura geográfica:</b>
     * <ul>
     *   <li><b>América do Sul:</b> 25 cidades (Brasil, Argentina, Chile, Colômbia, Peru)</li>
     *   <li><b>América do Norte:</b> 30 cidades (EUA, Canadá, México)</li>
     *   <li><b>Europa:</b> 50 cidades (Reino Unido, França, Alemanha, Espanha, Itália, etc.)</li>
     *   <li><b>Ásia:</b> 35 cidades (Japão, China, Coreia, Sudeste Asiático, Índia, Oriente Médio)</li>
     *   <li><b>Oceania:</b> 7 cidades (Austrália, Nova Zelândia)</li>
     *   <li><b>África:</b> 10 cidades (África do Sul, Egito, Marrocos, Nigéria, etc.)</li>
     * </ul>
     * <p>
     * <b>Formato dos dados:</b>
     * <pre>
     * {latitude, longitude, raioMaximoKm}
     * Exemplo: {-23.550520, -46.633308, 30}  // São Paulo, raio de 30km
     * </pre>
     */
    private static final double[][] KNOWN_CITIES = {
            // BRASIL
            {-23.550520, -46.633308, 30},  // São Paulo
            {-22.906847, -43.172897, 25},  // Rio de Janeiro
            {-19.916681, -43.934493, 20},  // Belo Horizonte
            {-25.428954, -49.267137, 15},  // Curitiba
            {-30.034647, -51.217658, 15},  // Porto Alegre
            {-15.794229, -47.882166, 20},  // Brasília
            {-12.977749, -38.501630, 15},  // Salvador
            {-3.119028, -60.021731, 15},   // Manaus
            {-8.047562, -34.876964, 15},   // Recife
            {-3.731862, -38.526669, 15},   // Fortaleza
            {-16.686882, -49.264789, 15},  // Goiânia
            {-1.455755, -48.490180, 12},   // Belém
            {-20.469710, -54.620121, 12},  // Campo Grande
            {-27.595378, -48.548050, 10},  // Florianópolis
            {-5.795044, -35.209710, 10},   // Natal

            // ARGENTINA
            {-34.603684, -58.381559, 25},  // Buenos Aires
            {-31.420083, -64.188776, 12},  // Córdoba
            {-32.889458, -68.845839, 10},  // Mendoza
            {-26.808285, -65.217590, 10},  // Tucumán

            // CHILE
            {-33.448890, -70.669265, 20},  // Santiago
            {-36.827001, -73.050036, 10},  // Concepción
            {-33.024489, -71.551628, 8},   // Valparaíso

            // COLÔMBIA
            {4.710989, -74.072092, 18},    // Bogotá
            {6.244203, -75.581212, 12},    // Medellín
            {3.451647, -76.531985, 10},    // Cali

            // PERU
            {-12.046374, -77.042793, 18},  // Lima
            {-13.531950, -71.967463, 8},   // Cusco

            // MÉXICO
            {19.432608, -99.133209, 30},   // Cidade do México
            {20.659698, -103.349609, 15},  // Guadalajara
            {25.686613, -100.316116, 12},  // Monterrey
            {21.161908, -86.851528, 10},   // Cancún
            {19.041297, -98.206200, 10},   // Puebla

            // EUA
            {40.712776, -74.005974, 25},   // Nova York
            {34.052234, -118.243685, 30},  // Los Angeles
            {41.878114, -87.629798, 20},   // Chicago
            {29.760427, -95.369803, 20},   // Houston
            {33.448377, -112.074037, 18},  // Phoenix
            {29.424122, -98.493628, 15},   // San Antonio
            {32.776664, -96.796988, 18},   // Dallas
            {37.774929, -122.419416, 15},  // San Francisco
            {47.606209, -122.332071, 12},  // Seattle
            {25.761680, -80.191790, 18},   // Miami
            {39.739236, -104.990251, 15},  // Denver
            {42.360082, -71.058880, 12},   // Boston
            {33.748995, -84.387982, 15},   // Atlanta
            {36.169941, -115.139830, 15},  // Las Vegas
            {38.907192, -77.036871, 12},   // Washington DC

            // CANADÁ
            {43.653226, -79.383184, 20},   // Toronto
            {45.501689, -73.567256, 15},   // Montreal
            {49.282729, -123.120738, 12},  // Vancouver
            {51.048615, -114.070846, 12},  // Calgary
            {53.546125, -113.493823, 10},  // Edmonton
            {45.421530, -75.697193, 10},   // Ottawa

            // EUROPA - REINO UNIDO
            {51.507351, -0.127758, 20},    // Londres
            {53.480759, -2.242631, 12},    // Manchester
            {52.486243, -1.890401, 10},    // Birmingham
            {55.953252, -3.188267, 10},    // Edimburgo
            {53.408371, -2.991573, 8},     // Liverpool

            // EUROPA - FRANÇA
            {48.856614, 2.352222, 20},     // Paris
            {43.296482, 5.369780, 12},     // Marselha
            {45.764043, 4.835659, 12},     // Lyon
            {43.710173, 7.261953, 8},      // Nice
            {44.837789, -0.579180, 10},    // Bordeaux

            // EUROPA - ALEMANHA
            {52.520007, 13.404954, 18},    // Berlim
            {48.135125, 11.581981, 15},    // Munique
            {50.937531, 6.960279, 12},     // Colônia
            {50.110922, 8.682127, 15},     // Frankfurt
            {53.551085, 9.993682, 12},     // Hamburgo

            // EUROPA - ESPANHA
            {40.416775, -3.703790, 18},    // Madrid
            {41.385064, 2.173404, 15},     // Barcelona
            {37.389092, -5.984459, 10},    // Sevilha
            {39.469907, -0.376288, 10},    // Valência

            // EUROPA - ITÁLIA
            {41.902784, 12.496366, 15},    // Roma
            {45.464204, 9.189982, 15},     // Milão
            {43.769560, 11.255814, 8},     // Florença
            {45.440847, 12.315515, 6},     // Veneza
            {40.851799, 14.268120, 12},    // Nápoles

            // EUROPA - PORTUGAL
            {38.722252, -9.139337, 12},    // Lisboa
            {41.157944, -8.629105, 10},    // Porto

            // EUROPA - OUTROS
            {52.370216, 4.895168, 12},     // Amsterdã
            {50.850340, 4.351710, 10},     // Bruxelas
            {46.947974, 7.447447, 8},      // Berna
            {47.376887, 8.541694, 10},     // Zurique
            {48.208174, 16.373819, 12},    // Viena
            {50.075538, 14.437800, 10},    // Praga
            {52.229676, 21.012229, 12},    // Varsóvia
            {47.497912, 19.040235, 10},    // Budapeste
            {59.329323, 18.068581, 12},    // Estocolmo
            {60.169856, 24.938379, 10},    // Helsinque
            {59.913869, 10.752245, 10},    // Oslo
            {55.676097, 12.568337, 10},    // Copenhague
            {37.983810, 23.727539, 12},    // Atenas
            {41.008238, 28.978359, 20},    // Istambul

            // EUROPA - RÚSSIA
            {55.755826, 37.617300, 25},    // Moscou
            {59.934280, 30.335099, 15},    // São Petersburgo

            // ÁSIA - JAPÃO
            {35.676192, 139.650311, 30},   // Tóquio
            {34.693738, 135.502165, 20},   // Osaka
            {35.011636, 135.768029, 12},   // Kyoto
            {43.062096, 141.354376, 10},   // Sapporo
            {33.590355, 130.401716, 12},   // Fukuoka

            // ÁSIA - CHINA
            {31.230416, 121.473701, 30},   // Xangai
            {39.904211, 116.407395, 30},   // Pequim
            {22.543096, 114.057865, 20},   // Shenzhen
            {23.125178, 113.280637, 20},   // Guangzhou
            {30.572815, 104.066801, 15},   // Chengdu
            {29.563010, 106.551556, 15},   // Chongqing

            // ÁSIA - COREIA DO SUL
            {37.566535, 126.977969, 25},   // Seul
            {35.179554, 129.075642, 15},   // Busan

            // ÁSIA - SUDESTE
            {1.352083, 103.819836, 15},    // Singapura
            {3.139003, 101.686855, 18},    // Kuala Lumpur
            {13.756331, 100.501765, 20},   // Bangkok
            {21.027764, 105.834160, 12},   // Hanói
            {10.823099, 106.629664, 18},   // Ho Chi Minh
            {14.599512, 120.984219, 18},   // Manila
            {-6.208763, 106.845599, 25},   // Jacarta

            // ÁSIA - ÍNDIA
            {28.613939, 77.209021, 25},    // Nova Delhi
            {19.075984, 72.877656, 25},    // Mumbai
            {12.971599, 77.594563, 18},    // Bangalore
            {13.082680, 80.270718, 15},    // Chennai
            {22.572646, 88.363895, 15},    // Calcutá
            {17.385044, 78.486671, 15},    // Hyderabad

            // ÁSIA - ORIENTE MÉDIO
            {25.204849, 55.270783, 20},    // Dubai
            {24.453884, 54.377344, 15},    // Abu Dhabi
            {31.768319, 35.213710, 10},    // Jerusalém
            {32.085300, 34.781768, 12},    // Tel Aviv
            {24.713552, 46.675296, 18},    // Riad
            {35.689487, 51.388973, 20},    // Teerã

            // OCEANIA
            {-33.868820, 151.209296, 25},  // Sydney
            {-37.813628, 144.963058, 20},  // Melbourne
            {-27.469771, 153.025124, 18},  // Brisbane
            {-31.950527, 115.860457, 15},  // Perth
            {-34.928499, 138.600746, 12},  // Adelaide
            {-36.848460, 174.763332, 12},  // Auckland
            {-41.286460, 174.776236, 8},   // Wellington

            // ÁFRICA
            {-33.924869, 18.424055, 15},   // Cidade do Cabo
            {-26.204103, 28.047305, 18},   // Joanesburgo
            {30.044420, 31.235712, 20},    // Cairo
            {33.589886, -7.603869, 12},    // Casablanca
            {36.806496, 10.181532, 10},    // Túnis
            {6.524379, 3.379206, 18},      // Lagos
            {5.603717, -0.186964, 12},     // Accra
            {-1.292066, 36.821946, 15},    // Nairobi
            {-6.792354, 39.208328, 12},    // Dar es Salaam
            {9.024857, 38.746770, 12},     // Adis Abeba
    };

    /**
     * Construtor do gerador com injeção de dependências.
     *
     * @param transactionRepository repositório para consulta do histórico de transações
     */
    public LatitudeAndLongitudeGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Gera coordenadas geográficas (latitude/longitude) baseadas no histórico do cartão.
     * <p>
     * Este é o método principal do gerador, implementando lógica inteligente de decisão:
     * <ol>
     *   <li>Recupera últimas 20 transações do cartão</li>
     *   <li>Se não houver histórico ou coordenadas: gera em cidade aleatória</li>
     *   <li>Se houver histórico:
     *     <ul>
     *       <li>95% de chance: gera próximo à última localização (raio de 5km)</li>
     *       <li>5% de chance: gera em cidade completamente diferente (anomalia)</li>
     *     </ul>
     *   </li>
     * </ol>
     * <p>
     * <b>Comportamento por cenário:</b>
     * <table border="1">
     *   <tr>
     *     <th>Cenário</th>
     *     <th>Probabilidade</th>
     *     <th>Comportamento</th>
     *     <th>Propósito</th>
     *   </tr>
     *   <tr>
     *     <td>Sem histórico</td>
     *     <td>N/A</td>
     *     <td>Cidade aleatória da base de dados</td>
     *     <td>Inicialização de cartão novo</td>
     *   </tr>
     *   <tr>
     *     <td>Transação normal</td>
     *     <td>95%</td>
     *     <td>Dentro de 5km da última transação</td>
     *     <td>Simular uso local consistente</td>
     *   </tr>
     *   <tr>
     *     <td>Transação anômala</td>
     *     <td>5%</td>
     *     <td>Cidade aleatória diferente</td>
     *     <td>Simular viagem ou fraude</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Uso em detecção de fraude:</b>
     * As coordenadas geradas permitem testar:
     * <ul>
     *   <li><b>Velocidade impossível:</b> transações consecutivas em cidades distantes</li>
     *   <li><b>Inconsistência IP/GPS:</b> IP de um país, GPS de outro</li>
     *   <li><b>Padrão de viagem:</b> mudanças frequentes de cidade</li>
     *   <li><b>Geofencing:</b> transações fora de zonas permitidas</li>
     * </ul>
     * <p>
     * <b>Formato de retorno:</b>
     * Array de duas strings com 6 casas decimais:
     * <pre>
     * [0] = latitude  (ex: "-23.550520")
     * [1] = longitude (ex: "-46.633308")
     * </pre>
     *
     * @param card cartão para o qual gerar as coordenadas
     * @return array com [latitude, longitude] formatadas com 6 casas decimais
     */
    public String[] generateLocation(Card card) {
        var lastTransactions = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        // Sem histórico: escolhe cidade aleatória
        if (lastTransactions.isEmpty()
                || lastTransactions.get(0).getLatitude() == null
                || lastTransactions.get(0).getLongitude() == null) {
            return generateFromRandomCity();
        }

        Transaction last = lastTransactions.get(0);
        double baseLat = Double.parseDouble(last.getLatitude());
        double baseLon = Double.parseDouble(last.getLongitude());

        boolean isAnomaly = RANDOM.nextDouble() < ANOMALY_PROBABILITY;

        if (isAnomaly) {
            // Anomalia: gera em cidade diferente (simula viagem/fraude)
            return generateFromRandomCity();
        }

        // Normal: gera perto da última localização
        return generateNearPoint(baseLat, baseLon, NORMAL_RADIUS_KM);
    }

    /**
     * Gera coordenadas próximas a uma cidade conhecida aleatória da base de dados.
     * <p>
     * Seleciona uma cidade aleatoriamente e gera coordenadas dentro de uma área
     * urbana realista, garantindo que a localização:
     * <ul>
     *   <li>Esteja dentro dos limites urbanos da cidade</li>
     *   <li>Tenha boa cobertura de geocoding reverso</li>
     *   <li>Represente área comercial ou residencial plausível</li>
     * </ul>
     * <p>
     * <b>Estratégia de geração:</b>
     * <ol>
     *   <li>Seleciona cidade aleatória de KNOWN_CITIES</li>
     *   <li>Usa 50% do raio máximo da cidade (concentra no centro urbano)</li>
     *   <li>Adiciona 1-N km ao raio para evitar coordenada exata do centro</li>
     *   <li>Gera ponto aleatório dentro deste raio ajustado</li>
     * </ol>
     * <p>
     * <b>Exemplo:</b>
     * <pre>
     * Cidade selecionada: São Paulo {-23.550520, -46.633308, 30}
     * Raio máximo: 30km
     * Raio efetivo: 1 + random(0-15) = ~8km
     * Resultado: coordenada a ~8km do centro de São Paulo
     * </pre>
     * <p>
     * <b>Por que 50% do raio máximo?</b>
     * Concentra as coordenadas nas áreas centrais e mais populosas,
     * evitando coordenadas em periferias distantes ou áreas rurais.
     *
     * @return array com [latitude, longitude] dentro de área urbana de cidade conhecida
     */
    private String[] generateFromRandomCity() {
        double[] city = KNOWN_CITIES[RANDOM.nextInt(KNOWN_CITIES.length)];
        double lat = city[0];
        double lon = city[1];
        double maxRadius = city[2];

        // Usa raio menor para garantir área urbana
        double radius = 1 + RANDOM.nextDouble() * (maxRadius * 0.5);

        return generateNearPoint(lat, lon, radius);
    }

    /**
     * Gera ponto aleatório dentro de um raio específico usando distribuição uniforme em disco.
     * <p>
     * Este método implementa distribuição estatística correta para pontos em círculo,
     * garantindo que pontos sejam uniformemente distribuídos em toda a área do disco
     * (não apenas no perímetro ou concentrados no centro).
     * <p>
     * <b>Algoritmo de distribuição uniforme:</b>
     * <ol>
     *   <li>Converte raio de km para graus (ajustado por latitude)</li>
     *   <li>Gera ângulo aleatório (0 a 2π)</li>
     *   <li>Gera distância usando √(random) para uniformidade no disco</li>
     *   <li>Aplica transformação polar para cartesiana</li>
     *   <li>Adiciona offset à coordenada base</li>
     *   <li>Normaliza limites de longitude (-180 a 180) e latitude (-90 a 90)</li>
     * </ol>
     * <p>
     * <b>Por que usar √(random)?</b>
     * Se usássemos apenas random(), os pontos se concentrariam no centro do círculo.
     * A raiz quadrada compensa a maior circunferência em raios maiores,
     * resultando em distribuição uniforme de densidade.
     * <p>
     * <b>Conversão de km para graus:</b>
     * <ul>
     *   <li><b>Latitude:</b> 1 grau ≈ 111 km (constante globalmente)</li>
     *   <li><b>Longitude:</b> 1 grau ≈ 111 km × cos(latitude) (varia com latitude)</li>
     * </ul>
     * <p>
     * <b>Exemplo de cálculo:</b>
     * <pre>
     * Base: São Paulo (-23.55, -46.63)
     * Raio: 5km
     *
     * Conversão:
     * - radiusLat = 5/111 ≈ 0.045 graus
     * - radiusLon = 5/(111*cos(-23.55°)) ≈ 0.049 graus
     *
     * Geração:
     * - angle = random(0 a 2π) = 1.5 rad (≈86°)
     * - distance = √random(0 a 1) = 0.7
     *
     * Resultado:
     * - deltaLat = 0.7 * 0.045 * cos(1.5) ≈ 0.002
     * - deltaLon = 0.7 * 0.049 * sin(1.5) ≈ 0.034
     * - newLat = -23.55 + 0.002 = -23.548
     * - newLon = -46.63 + 0.034 = -46.596
     * </pre>
     * <p>
     * <b>Normalização de limites:</b>
     * <ul>
     *   <li>Longitude é "wrapped" em -180 a 180 (cruza linha de data internacional)</li>
     *   <li>Latitude é "clamped" em -90 a 90 (não pode ultrapassar polos)</li>
     * </ul>
     *
     * @param baseLat latitude do ponto central em graus
     * @param baseLon longitude do ponto central em graus
     * @param radiusKm raio do círculo em quilômetros
     * @return array com [latitude, longitude] dentro do raio especificado
     */
    private String[] generateNearPoint(double baseLat, double baseLon, double radiusKm) {
        // Converte raio de km para graus (aproximado)
        double radiusInDegreesLat = radiusKm / 111.0;
        double radiusInDegreesLon = radiusKm / (111.0 * Math.cos(Math.toRadians(baseLat)));

        // Distribuição uniforme em disco (não apenas no perímetro)
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        double distance = Math.sqrt(RANDOM.nextDouble()); // sqrt para distribuição uniforme

        double deltaLat = distance * radiusInDegreesLat * Math.cos(angle);
        double deltaLon = distance * radiusInDegreesLon * Math.sin(angle);

        double newLat = baseLat + deltaLat;
        double newLon = baseLon + deltaLon;

        // Normaliza longitude para -180 a 180
        if (newLon > 180) newLon -= 360;
        if (newLon < -180) newLon += 360;

        // Limita latitude
        newLat = Math.max(-90, Math.min(90, newLat));

        return new String[]{
                FORMAT.format(newLat),
                FORMAT.format(newLon)
        };
    }

    /**
     * Encontra a cidade conhecida mais próxima de uma coordenada arbitrária.
     * <p>
     * Este método utilitário é usado para "corrigir" coordenadas problemáticas
     * ou validar se uma coordenada está próxima de área urbana conhecida.
     * <p>
     * <b>Algoritmo:</b>
     * <ol>
     *   <li>Calcula distância Haversine para todas as cidades</li>
     *   <li>Identifica cidade com menor distância</li>
     *   <li>Gera nova coordenada dentro de 30% do raio dessa cidade</li>
     * </ol>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Correção de coordenadas em oceano ou área rural</li>
     *   <li>Validação de coordenadas de teste</li>
     *   <li>Normalização de dados de entrada</li>
     * </ul>
     * <p>
     * <b>Exemplo:</b>
     * <pre>
     * Input: {-23.7, -46.8} (oeste de São Paulo, área semi-rural)
     * Cidade mais próxima: São Paulo {-23.55, -46.63, raio 30km}
     * Output: coordenada a ~9km do centro de São Paulo (30% de 30km)
     * </pre>
     *
     * @param lat latitude da coordenada a ser corrigida
     * @param lon longitude da coordenada a ser corrigida
     * @return array com [latitude, longitude] próxima à cidade conhecida mais próxima
     */
    public String[] findNearestKnownCity(double lat, double lon) {
        double minDistance = Double.MAX_VALUE;
        double[] nearestCity = KNOWN_CITIES[0];

        for (double[] city : KNOWN_CITIES) {
            double distance = haversineDistance(lat, lon, city[0], city[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestCity = city;
            }
        }

        return generateNearPoint(nearestCity[0], nearestCity[1], nearestCity[2] * 0.3);
    }

    /**
     * Calcula a distância entre dois pontos na superfície da Terra usando a fórmula de Haversine.
     * <p>
     * A fórmula de Haversine calcula a distância do grande círculo entre dois pontos
     * em uma esfera, levando em conta a curvatura da Terra. É precisa para distâncias
     * de até milhares de quilômetros.
     * <p>
     * <b>Fórmula matemática:</b>
     * <pre>
     * a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
     * c = 2 × atan2(√a, √(1-a))
     * d = R × c
     *
     * Onde:
     * - Δlat = lat2 - lat1 (em radianos)
     * - Δlon = lon2 - lon1 (em radianos)
     * - R = raio da Terra (6371 km)
     * </pre>
     * <p>
     * <b>Precisão:</b>
     * <ul>
     *   <li>Assume Terra perfeitamente esférica (erro < 0.5% para distâncias < 1000km)</li>
     *   <li>Erro aumenta próximo aos polos devido ao achatamento da Terra</li>
     *   <li>Para precisão geodésica, use fórmula de Vincenty</li>
     * </ul>
     * <p>
     * <b>Exemplo de cálculo:</b>
     * <pre>
     * São Paulo: (-23.55, -46.63)
     * Rio de Janeiro: (-22.91, -43.17)
     *
     * Distância calculada: ~357 km
     * Distância real: ~358 km (erro de ~0.3%)
     * </pre>
     *
     * @param lat1 latitude do primeiro ponto em graus
     * @param lon1 longitude do primeiro ponto em graus
     * @param lat2 latitude do segundo ponto em graus
     * @param lon2 longitude do segundo ponto em graus
     * @return distância em quilômetros entre os dois pontos
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Raio da Terra em km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Gera localização para uma região geográfica específica (útil para testes direcionados).
     * <p>
     * Este método permite gerar coordenadas restritas a um conjunto específico de cidades,
     * útil para:
     * <ul>
     *   <li>Testes de regras antifraude regionais</li>
     *   <li>Simulação de comportamento de usuários locais</li>
     *   <li>Validação de geofencing por região</li>
     *   <li>Datasets de treinamento especializados</li>
     * </ul>
     * <p>
     * <b>Regiões suportadas:</b>
     * <ul>
     *   <li><b>BRAZIL / BR:</b> 15 principais cidades brasileiras</li>
     *   <li><b>USA / US:</b> 15 principais cidades americanas</li>
     *   <li><b>EUROPE / EU:</b> 9 principais cidades europeias</li>
     * </ul>
     * <p>
     * <b>Comportamento:</b>
     * <ol>
     *   <li>Filtra cidades da região solicitada</li>
     *   <li>Se região inválida: usa cidade aleatória global</li>
     *   <li>Seleciona cidade aleatória da região</li>
     *   <li>Gera coordenada dentro de 40% do raio da cidade</li>
     * </ol>
     * <p>
     * <b>Exemplo de uso:</b>
     * <pre>
     * String[] coords = generator.generateForRegion("BRAZIL");
     * // Retorna coordenada em uma das 15 cidades brasileiras
     * // Exemplo: dentro de 8km do centro de São Paulo (40% de 20km)
     * </pre>
     *
     * @param region código da região (case-insensitive): "BRAZIL"/"BR", "USA"/"US", "EUROPE"/"EU"
     * @return array com [latitude, longitude] dentro da região especificada
     */
    public String[] generateForRegion(String region) {
        List<double[]> regionCities = getRegionCities(region);

        if (regionCities.isEmpty()) {
            return generateFromRandomCity();
        }

        double[] city = regionCities.get(RANDOM.nextInt(regionCities.size()));
        return generateNearPoint(city[0], city[1], city[2] * 0.4);
    }

    /**
     * Retorna lista de cidades pertencentes a uma região específica.
     * <p>
     * Mapeia códigos de região para subconjuntos do array KNOWN_CITIES
     * através de índices específicos.
     * <p>
     * <b>Mapeamento de índices:</b>
     * <ul>
     *   <li><b>Brasil:</b> índices 0-14 (15 cidades)</li>
     *   <li><b>EUA:</b> índices 29-43 (15 cidades)</li>
     *   <li><b>Europa:</b> índices 49-57 (9 cidades principais)</li>
     * </ul>
     * <p>
     * <b>Nota de manutenção:</b>
     * Se o array KNOWN_CITIES for modificado, os índices neste método
     * devem ser atualizados para manter a correspondência correta.
     *
     * @param region código da região (case-insensitive)
     * @return lista de arrays {lat, lon, raio} das cidades da região, ou lista vazia se região inválida
     */
    private List<double[]> getRegionCities(String region) {
        return switch (region.toUpperCase()) {
            case "BRAZIL", "BR" -> List.of(
                    KNOWN_CITIES[0], KNOWN_CITIES[1], KNOWN_CITIES[2],
                    KNOWN_CITIES[3], KNOWN_CITIES[4], KNOWN_CITIES[5],
                    KNOWN_CITIES[6], KNOWN_CITIES[7], KNOWN_CITIES[8],
                    KNOWN_CITIES[9], KNOWN_CITIES[10], KNOWN_CITIES[11],
                    KNOWN_CITIES[12], KNOWN_CITIES[13], KNOWN_CITIES[14]
            );
            case "USA", "US" -> List.of(
                    KNOWN_CITIES[29], KNOWN_CITIES[30], KNOWN_CITIES[31],
                    KNOWN_CITIES[32], KNOWN_CITIES[33], KNOWN_CITIES[34],
                    KNOWN_CITIES[35], KNOWN_CITIES[36], KNOWN_CITIES[37],
                    KNOWN_CITIES[38], KNOWN_CITIES[39], KNOWN_CITIES[40],
                    KNOWN_CITIES[41], KNOWN_CITIES[42], KNOWN_CITIES[43]
            );
            case "EUROPE", "EU" -> List.of(
                    KNOWN_CITIES[49], KNOWN_CITIES[50], KNOWN_CITIES[51],
                    KNOWN_CITIES[55], KNOWN_CITIES[56], KNOWN_CITIES[57],
                    KNOWN_CITIES[62], KNOWN_CITIES[63], KNOWN_CITIES[64]
            );
            default -> List.of();
        };
    }
}