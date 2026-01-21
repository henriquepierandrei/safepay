package tech.safepay.dtos.transaction;

public record ResolvedLocalizationDto(
        String countryCode,
        String state,
        String city
) {
}
