package tech.safepay.configs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Componente responsável por resolver informações de localização
 * (país, estado e cidade) a partir de coordenadas geográficas
 * utilizando o serviço Nominatim (OpenStreetMap).
 *
 * <p>
 * Esta classe é usada como apoio ao pipeline antifraude.
 * Falhas externas não devem interromper o fluxo principal.
 * </p>
 */
@Component
public class ResolveLocalizationConfig {

    /**
     * Endpoint do serviço Nominatim para reverse geocoding.
     *
     * <p>
     * Recebe latitude e longitude e retorna um JSON
     * com dados de endereço normalizados.
     * </p>
     */
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";

    /**
     * Cliente HTTP nativo do Java.
     *
     * <p>
     * Thread-safe e reutilizável.
     * Evita dependência externa desnecessária (RestTemplate/WebClient).
     * </p>
     */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * ObjectMapper utilizado para parsing do JSON retornado
     * pela API de geolocalização.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resolve informações de localização a partir de latitude e longitude.
     *
     * <p>
     * O método realiza:
     * </p>
     * <ul>
     *   <li>Chamada HTTP para o serviço Nominatim</li>
     *   <li>Extração segura dos campos de endereço</li>
     *   <li>Normalização do código do país (ISO 3166-1 alpha-2)</li>
     * </ul>
     *
     * <p>
     * Em caso de erro (timeout, parsing, indisponibilidade externa),
     * o método retorna um DTO vazio para não comprometer
     * o pipeline antifraude.
     * </p>
     *
     * @param latitude latitude do ponto geográfico
     * @param longitude longitude do ponto geográfico
     * @return {@link ResolvedLocalizationDto} contendo país, estado e cidade
     */
    public ResolvedLocalizationDto resolve(String latitude, String longitude) {

        try {
            String url = String.format(NOMINATIM_URL, latitude, longitude);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SafePay-Fraud-Service/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode address = objectMapper
                    .readTree(response.body())
                    .path("address");

            String city = getCity(address);
            String state = address.path("state").asText(null);

            /**
             * Código do país no padrão ISO 3166-1 alpha-2.
             * A API retorna em lowercase, então normalizamos.
             */
            String countryCode = address
                    .path("country_code")
                    .asText(null);

            if (countryCode != null) {
                countryCode = countryCode.toUpperCase();
            }

            return new ResolvedLocalizationDto(countryCode, state, city);

        } catch (Exception e) {
            /**
             * Falha de geolocalização não deve quebrar
             * o fluxo principal da aplicação.
             */
            return new ResolvedLocalizationDto(null, null, null);
        }
    }

    /**
     * Resolve o nome da cidade a partir do nó de endereço.
     *
     * <p>
     * O Nominatim pode retornar diferentes chaves
     * dependendo da região:
     * </p>
     * <ul>
     *   <li>city</li>
     *   <li>town</li>
     *   <li>village</li>
     * </ul>
     *
     * @param address nó JSON contendo os dados de endereço
     * @return nome da cidade ou {@code null} se não encontrado
     */
    private String getCity(JsonNode address) {
        if (!address.path("city").isMissingNode()) {
            return address.path("city").asText();
        }
        if (!address.path("town").isMissingNode()) {
            return address.path("town").asText();
        }
        if (!address.path("village").isMissingNode()) {
            return address.path("village").asText();
        }
        return null;
    }
}
