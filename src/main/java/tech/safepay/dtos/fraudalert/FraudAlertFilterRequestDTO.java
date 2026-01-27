package tech.safepay.dtos.fraudalert;

import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FraudAlertFilterRequestDTO(
        Boolean recentAlerts,
        Severity severity,
        Integer startFraudScore,
        Integer endFraudScore,
        List<AlertType> alertTypeList,
        LocalDateTime createdAtFrom,
        UUID transactionId,
        UUID cardId,
        UUID deviceId,
        Integer page,
        Integer size
) {
}
