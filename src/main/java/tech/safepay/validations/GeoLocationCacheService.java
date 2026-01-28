package tech.safepay.validations;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tech.safepay.configs.ResolveLocalizationConfig;

/**
 * Serviço de cache para resolução de geolocalização.
 * <p>
 * Este serviço fornece uma camada de cache para operações de resolução de coordenadas
 * geográficas em códigos de país, otimizando o desempenho ao evitar chamadas repetidas
 * para o mesmo par de coordenadas.
 * </p>
 * <p>
 * O cache é implementado usando a abstração de cache do Spring, permitindo armazenamento
 * em memória ou distribuído conforme configuração da aplicação.
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 * @see ResolveLocalizationConfig
 */
@Service
public class GeoLocationCacheService {

    /**
     * Configuração responsável pela resolução efetiva de coordenadas para país.
     */
    private final ResolveLocalizationConfig resolveLocalizationConfig;

    /**
     * Construtor para injeção de dependências.
     *
     * @param resolveLocalizationConfig configuração de resolução de localização
     */
    public GeoLocationCacheService(ResolveLocalizationConfig resolveLocalizationConfig) {
        this.resolveLocalizationConfig = resolveLocalizationConfig;
    }

    /**
     * Resolve o código do país com base em coordenadas geográficas, utilizando cache.
     * <p>
     * Este método realiza a conversão de latitude e longitude para código de país (ISO 3166-1 alpha-2),
     * armazenando o resultado em cache para melhorar o desempenho em consultas subsequentes
     * das mesmas coordenadas.
     * </p>
     * <p>
     * <strong>Estratégia de Cache:</strong>
     * <ul>
     *   <li>Nome do cache: "geoLocationCache"</li>
     *   <li>Chave: concatenação de latitude e longitude separadas por ':'</li>
     *   <li>Formato da chave: "latitude:longitude"</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Exemplo de uso:</strong>
     * <pre>
     * String country = geoLocationCacheService.resolveCountry("-23.5505", "-46.6333");
     * // Retorna: "BR" (código do Brasil)
     * </pre>
     * </p>
     *
     * @param lat latitude em formato String (ex: "-23.5505")
     * @param lon longitude em formato String (ex: "-46.6333")
     * @return código do país no formato ISO 3166-1 alpha-2 (ex: "BR", "US", "JP"),
     *         ou {@code null} se não for possível resolver as coordenadas
     * @see ResolveLocalizationConfig#resolve(String, String)
     */
    @Cacheable(
            value = "geoLocationCache",
            key = "#lat + ':' + #lon"
    )
    public String resolveCountry(String lat, String lon) {
        var response = resolveLocalizationConfig.resolve(lat, lon);
        return response != null ? response.countryCode() : null;
    }
}