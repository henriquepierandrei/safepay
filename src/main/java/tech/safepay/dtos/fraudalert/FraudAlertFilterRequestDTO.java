package tech.safepay.dtos.fraudalert;

import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de requisição para filtros de alertas de fraude.
 * <p>
 * Utilizado em endpoints de consulta para aplicar critérios
 * dinâmicos de filtragem sobre alertas gerados pelo sistema.
 * </p>
 * <p>
 * Todos os campos são opcionais. Apenas os filtros informados
 * serão considerados na consulta.
 * </p>
 *
 * @param recentAlerts indica se devem ser considerados apenas alertas recentes
 * @param severity nível de severidade do alerta
 * @param startFraudScore score mínimo de fraude
 * @param endFraudScore score máximo de fraude
 * @param alertTypeList lista de tipos de alerta
 * @param createdAtFrom data/hora inicial de criação do alerta
 * @param transactionId identificador da transação associada
 * @param cardId identificador do cartão associado
 * @param deviceId identificador do dispositivo associado
 *
 * @author SafePay Team
 * @version 1.0
 */
public record FraudAlertFilterRequestDTO(
        Boolean recentAlerts,
        Severity severity,
        Integer startFraudScore,
        Integer endFraudScore,
        List<AlertType> alertTypeList,
        LocalDateTime createdAtFrom,
        UUID transactionId,
        UUID cardId,
        UUID deviceId
) {
}
