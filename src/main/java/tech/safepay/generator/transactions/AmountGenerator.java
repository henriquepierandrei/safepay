package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Component
public class AmountGenerator {
    // Iniciando variável estática
    private static final Random RANDOM = new Random();

    private final TransactionRepository transactionRepository;

    public AmountGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    private



    /**
     * Calcula a média de transações para não sair do padrão.
     * @param transactions
     * @return
     */
    private BigDecimal calculateAverageAmount(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.valueOf(100); // valor base inicial
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
                BigDecimal.valueOf(transactions.size()),
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal generateNormalAmount(BigDecimal average) {
        double variation = 0.9 + (RANDOM.nextDouble() * 0.2); // 0.9–1.1
        return average.multiply(BigDecimal.valueOf(variation))
                .setScale(2, RoundingMode.HALF_UP);
    }


    public BigDecimal generateAmount(Card card) {

        var lastTx = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);
        var average = calculateAverageAmount(lastTx);

        // 90% normal
        if (RANDOM.nextDouble() < 0.9) {
            return generateNormalAmount(average);
        }

        // 10% fora do padrão
        return average.multiply(BigDecimal.valueOf(3 + RANDOM.nextInt(3)))
                .setScale(2, RoundingMode.HALF_UP);
    }






}
