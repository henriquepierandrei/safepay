package tech.safepay.dtos.fraudalert;

import tech.safepay.Enums.AlertStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudStatusResponseDto(
        UUID alertId,
        AlertStatus alertStatus,
        BigDecimal amount,
        Boolean isReimbursement
) {
}
