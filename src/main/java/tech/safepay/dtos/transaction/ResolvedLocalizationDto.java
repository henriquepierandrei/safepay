package tech.safepay.dtos.transaction;

/**
 * DTO que representa a localização resolvida de uma transação.
 * <p>
 * Normalmente utilizado após processos de geolocalização
 * (ex: IP lookup ou serviços externos de localização),
 * convertendo coordenadas ou IP em dados legíveis.
 * </p>
 *
 * @param countryCode código do país (ISO-3166)
 * @param state estado ou região administrativa
 * @param city cidade associada à localização
 *
 * @author SafePay Team
 * @version 1.0
 */
public record ResolvedLocalizationDto(
        String countryCode,
        String state,
        String city
) {
}
