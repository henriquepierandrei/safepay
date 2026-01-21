package tech.safepay.configs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ResolveLocalizationConfig {


    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();



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

            // ISO 3166-1 alpha-2 (normalizado)
            String countryCode = address
                    .path("country_code")
                    .asText(null);

            if (countryCode != null) {
                countryCode = countryCode.toUpperCase();
            }

            return new ResolvedLocalizationDto(countryCode, state, city);

        } catch (Exception e) {
            // Falha de geolocalização não pode quebrar o pipeline antifraude
            return new ResolvedLocalizationDto(null, null, null);
        }
    }

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
