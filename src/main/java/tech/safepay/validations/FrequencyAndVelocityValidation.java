package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.util.List;

@Component
public class FrequencyAndVelocityValidation {


    /**
     * =========================
     * VELOCITY_ABUSE
     * =========================
     *
     * Objetivo:
     * Detectar muitas transações em um curto intervalo de tempo,
     * padrão típico de card testing, bots ou ataques automatizados.
     *
     * Estratégia:
     * - Janela móvel de 5 minutos
     * - Contagem simples de transações
     * - Threshold fixo definido por política de risco
     *
     * Peso: 35
     * Sinal forte, frequentemente associado a fraude real.
     */
    public ValidationResultDto velocityAbuseValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Janela de observação (últimos 5 minutos)
        List<Transaction> recentTransactions = snapshot.last5Minutes();


        int maxAllowed = 3;

        if (recentTransactions.size() >= maxAllowed) {
            result.addScore(AlertType.VELOCITY_ABUSE.getScore());
            result.addAlert(AlertType.VELOCITY_ABUSE);
        }

        return result;
    }

    /**
     * =========================
     * BURST_ACTIVITY
     * =========================
     *
     * Objetivo:
     * Identificar picos súbitos de atividade que fogem do padrão
     * histórico do cartão, mesmo quando o volume absoluto não é alto.
     *
     * Estratégia:
     * 1. Baseline comportamental (últimas 24 horas)
     * 2. Cálculo da média de transações por hora
     * 3. Janela curta de observação (5 minutos)
     * 4. Comparação entre volume esperado vs volume real
     *
     * Peso: 25
     * Atua como reforço para VELOCITY_ABUSE.
     */
    public ValidationResultDto burstActivityValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Baseline histórico (últimas 24 horas)

        List<Transaction> baselineTransactions = snapshot.last24Hours();

        if (baselineTransactions.size() < 5) return result;

        // Média histórica de transações por hora
        double avgPerHour = baselineTransactions.size() / 24.0;

        int burstCount = snapshot.last5Minutes().size();

        if (burstCount > avgPerHour * 3) {
            result.addScore(AlertType.BURST_ACTIVITY.getScore());
            result.addAlert(AlertType.BURST_ACTIVITY);
        }


        return result;
    }
}
