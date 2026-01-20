package tech.safepay.dtos.transaction;

import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.device.DeviceListResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        CardDataResponseDto card,
        MerchantCategory merchantCategory,
        BigDecimal amount,
        LocalDateTime transactionDateAndTime,
        String latitude,
        String longitude,
        DeviceListResponseDto.DeviceDto device,
        String ipAddress,
        TransactionStatus transactionStatus,
        Boolean isFraud,
        LocalDateTime createdAt
) {

}
