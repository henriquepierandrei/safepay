package tech.safepay.dtos.transaction;

import tech.safepay.Enums.MerchantCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ManualTransactionDto(
        UUID cardId,
        UUID deviceId,
        BigDecimal amount,
        MerchantCategory merchantCategory,
        String ipAddress,
        String latitude,
        String longitude
) {
}
