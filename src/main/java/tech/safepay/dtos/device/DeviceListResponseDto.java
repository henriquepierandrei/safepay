package tech.safepay.dtos.device;

import tech.safepay.Enums.DeviceType;

import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta para listagem de dispositivos.
 * <p>
 * Utilizado em endpoints que retornam dispositivos cadastrados
 * no sistema SafePay, normalmente de forma paginada.
 * </p>
 *
 * @param devices lista de dispositivos retornados
 *
 * @author SafePay Team
 * @version 1.0
 */
public record DeviceListResponseDto(
        List<DeviceDto> devices
) {

    /**
     * DTO interno que representa os dados essenciais de um dispositivo.
     *
     * @param id identificador único do dispositivo
     * @param fingerPrintId identificador de fingerprint do dispositivo
     * @param deviceType tipo do dispositivo
     * @param os sistema operacional do dispositivo
     * @param browser navegador utilizado no dispositivo
     */
    public record DeviceDto(
            UUID id,
            String fingerPrintId,
            DeviceType deviceType,
            String os,
            String browser
    ) {
    }
}
