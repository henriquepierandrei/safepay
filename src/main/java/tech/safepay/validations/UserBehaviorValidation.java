package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UserBehaviorValidation {

    private final TransactionRepository transactionRepository;

    public UserBehaviorValidation(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * TIME_OF_DAY_ANOMALY
     *
     * Objetivo:
     * Detectar transações realizadas em horários atípicos
     * quando comparadas ao comportamento histórico do cartão.
     *
     * Peso baixo (10):
     * - Não bloqueia sozinho
     * - Serve como reforço para outros sinais (velocity, location, device)
     */
    public Integer timeOfDayAnomaly(Transaction transaction) {

        Card card = transaction.getCard();

        // 1. Define o baseline: últimos 30 dias
        LocalDateTime baselineStart = LocalDateTime.now().minusDays(30);

        List<Transaction> historicalTransactions =
                transactionRepository.findByCardAndCreatedAtAfter(card, baselineStart);

        // Histórico insuficiente → não decide nada
        if (historicalTransactions.size() < 10) {
            return 0;
        }

        // 2. Calcula a média do horário das transações (em horas)
        double averageHour = historicalTransactions.stream()
                .mapToInt(t -> t.getCreatedAt().getHour())
                .average()
                .orElse(transaction.getCreatedAt().getHour());

        // 3. Horário da transação atual
        int currentHour = transaction.getCreatedAt().getHour();

        // 4. Define tolerância (desvio aceitável)
        int allowedDeviation = 4; // horas

        // 5. Verifica anomalia
        if (Math.abs(currentHour - averageHour) > allowedDeviation){
            return AlertType.TIME_OF_DAY_ANOMALY.getScore();
        }
        return 0;
    }
}
