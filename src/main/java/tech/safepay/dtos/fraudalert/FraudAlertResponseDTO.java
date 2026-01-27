package tech.safepay.dtos.fraudalert;

import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.entities.FraudAlert;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAlertResponseDTO(
        List<AlertType> alertTypeList,
        Severity severity,
        Integer fraudProbability,
        String description,
        AlertStatus status,
        LocalDateTime createdAt,
        Integer fraudScore
) {

    public static FraudAlertResponseDTO from(FraudAlert entity) {
        return new FraudAlertResponseDTO(
                entity.getAlertTypes(),
                entity.getSeverity(),
                entity.getFraudProbability(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getFraudScore()
        );
    }
}
