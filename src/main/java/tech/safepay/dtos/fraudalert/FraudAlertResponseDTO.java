package tech.safepay.dtos.fraudalert;

import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.entities.FraudAlert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta para alertas de fraude.
 * <p>
 * Representa a visão consolidada de um alerta gerado pelo sistema,
 * exposta via API para consumo por clientes internos ou externos.
 * </p>
 *
 * @param alertTypeList lista de tipos de alerta associados
 * @param severity nível de severidade do alerta
 * @param fraudProbability probabilidade estimada de fraude (percentual)
 * @param description descrição detalhada do alerta
 * @param status status atual do alerta
 * @param createdAt data e hora de criação do alerta
 * @param fraudScore score numérico de risco de fraude
 *
 * @author SafePay Team
 * @version 1.0
 */
public record FraudAlertResponseDTO(
        List<AlertType> alertTypeList,
        Severity severity,
        Integer fraudProbability,
        String description,
        AlertStatus status,
        LocalDateTime createdAt,
        Integer fraudScore
) {

    /**
     * Converte a entidade {@link FraudAlert} para o DTO de resposta.
     *
     * @param entity entidade de alerta de fraude
     * @return DTO preenchido com dados da entidade
     */
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
