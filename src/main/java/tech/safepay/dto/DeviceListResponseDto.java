package tech.safepay.dto;

import tech.safepay.Enums.DeviceType;

import java.util.List;
import java.util.UUID;

public record DeviceListResponseDto(
        List<DeviceDto> devices

) {
    public record DeviceDto(
            UUID id,
            String fingerPrintId,
            DeviceType deviceType,
            String os,
            String browser   ){}
}

