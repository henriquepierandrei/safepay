package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.util.List;

@Component
public class UserBehaviorValidation {
    /**
     * TIME_OF_DAY_ANOMALY
     *
     * Objetivo:
     * Detectar transações realizadas em horários atípicos
     * comparadas ao comportamento histórico recente.
     *
     * Estratégia:
     * - Usa histórico já carregado no ValidationContext
     * - Baseado nas últimas transações (proxy de 30 dias)
     *
     * Peso baixo (10):
     * - Sinal complementar
     * - Nunca decide sozinho
     */
    public ValidationResultDto timeOfDayAnomaly(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        if (transaction == null || transaction.getCreatedAt() == null) {
            return result;
        }

        List<Transaction> historicalTransactions = snapshot.last20();

        // Histórico insuficiente → risco neutro
        if (historicalTransactions.size() < 10) return result;

        // Média do horário histórico
        double averageHour = historicalTransactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .mapToInt(t -> t.getCreatedAt().getHour())
                .average()
                .orElse(transaction.getCreatedAt().getHour());

        int currentHour = transaction.getCreatedAt().getHour();

        int allowedDeviation = 4; // horas

        if (Math.abs(currentHour - averageHour) > allowedDeviation) {
            result.addScore(AlertType.TIME_OF_DAY_ANOMALY.getScore());
            result.addAlert(AlertType.TIME_OF_DAY_ANOMALY);
        }

        return result;
    }
}
