package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FrequencyAndVelocityValidation {

    private final TransactionRepository transactionRepository;

    public FrequencyAndVelocityValidation(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

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
    public Integer velocityAbuseValidation(Transaction transaction) {
        Card card = transaction.getCard();

        // Janela de observação (últimos 5 minutos)
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(5);

        List<Transaction> recentTransactions =
                transactionRepository.findByCardAndCreatedAtAfter(card, windowStart);

        // Limite máximo aceitável de transações na janela
        int maxAllowed = 3;

        if (recentTransactions.size() >= maxAllowed) {
            return AlertType.VELOCITY_ABUSE.getScore();
        }

        return 0;
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
    public Integer burstActivityValidation(Transaction transaction) {
        Card card = transaction.getCard();

        // Baseline histórico (últimas 24 horas)
        LocalDateTime baselineStart = LocalDateTime.now().minusHours(24);

        List<Transaction> baselineTransactions =
                transactionRepository.findByCardAndCreatedAtAfter(card, baselineStart);

        // Histórico insuficiente → risco neutro
        if (baselineTransactions.size() < 5) {
            return 0;
        }

        // Média histórica de transações por hora
        double avgPerHour = baselineTransactions.size() / 24.0;

        // Janela curta para detectar pico (últimos 5 minutos)
        LocalDateTime burstWindowStart = LocalDateTime.now().minusMinutes(5);

        int burstCount =
                transactionRepository.findByCardAndCreatedAtAfter(card, burstWindowStart).size();

        // Pico significativamente acima do padrão histórico
        if (burstCount > avgPerHour * 3) {
            return AlertType.BURST_ACTIVITY.getScore();
        }

        return 0;
    }
}
