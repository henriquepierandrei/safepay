package tech.safepay.validations;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tech.safepay.configs.ResolveLocalizationConfig;

@Service
public class GeoLocationCacheService {

    private final ResolveLocalizationConfig resolveLocalizationConfig;

    public GeoLocationCacheService(ResolveLocalizationConfig resolveLocalizationConfig) {
        this.resolveLocalizationConfig = resolveLocalizationConfig;
    }

    @Cacheable(
            value = "geoLocationCache",
            key = "#lat + ':' + #lon"
    )
    public String resolveCountry(String lat, String lon) {
        var response = resolveLocalizationConfig.resolve(lat, lon);
        return response != null ? response.countryCode() : null;
    }
}
