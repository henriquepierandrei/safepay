package tech.safepay.dtos.transaction;

import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.Severity;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.dtos.validation.ValidationResultDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionResponseDto(
        CardDataResponseDto card,
        MerchantCategory merchantCategory,
        BigDecimal amount,
        LocalDateTime transactionDateAndTime,

        String latitude,
        String longitude,
        ResolvedLocalizationDto localizationDto,

        ValidationResultDto validations,
        Severity severity,

        DeviceListResponseDto.DeviceDto device,
        String ipAddress,
        TransactionStatus transactionStatus,
        Boolean isFraud,
        LocalDateTime createdAt
) {
}
