package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Component
public class AmountGenerator {

    private static final Random RANDOM = new Random();
    private final TransactionRepository transactionRepository;

    public AmountGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Calcula a média das últimas transações.
     */
    private BigDecimal calculateAverageAmount(List<Transaction> transactions) {
        if (transactions.isEmpty()) return BigDecimal.valueOf(100); // valor base inicial

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(transactions.size()), RoundingMode.HALF_UP);
    }

    /**
     * Gera valor dentro do padrão (variação ±10%)
     */
    private BigDecimal generateNormalAmount(BigDecimal average) {
        double variation = 0.9 + (RANDOM.nextDouble() * 0.2); // 0.9–1.1
        return average.multiply(BigDecimal.valueOf(variation))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gera valor fora do padrão (10% de chance) mas respeitando limite do cartão
     */
    private BigDecimal generateHighAmount(BigDecimal average, Card card) {
        // multiplicador aleatório 3–5x a média
        BigDecimal highAmount = average.multiply(BigDecimal.valueOf(3 + RANDOM.nextInt(3)))
                .setScale(2, RoundingMode.HALF_UP);

        // 90% chance de respeitar remainingLimit, 10% chance de ultrapassar
        if (RANDOM.nextDouble() < 0.9) {
            return highAmount.min(card.getRemainingLimit());
        }

        return highAmount; // 10% chance de ultrapassar
    }

    /**
     * Gera valor final para a transação.
     */
    public BigDecimal generateAmount(Card card) {
        List<Transaction> lastTx = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);
        BigDecimal average = calculateAverageAmount(lastTx);

        if (RANDOM.nextDouble() < 0.9) {
            BigDecimal remaining = card.getRemainingLimit() != null ? card.getRemainingLimit() : card.getCreditLimit();
            if (remaining == null) remaining = BigDecimal.valueOf(1000);

            return generateNormalAmount(average).min(remaining);
        } else {
            return generateHighAmount(average, card);
        }
    }
}
