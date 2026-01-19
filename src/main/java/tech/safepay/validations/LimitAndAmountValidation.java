package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class LimitAndAmountValidation {

    private static final int SCALE = 2;

    // Multiplicador para considerar valor “significativamente acima da média”
    private static final BigDecimal HIGH_AMOUNT_MULTIPLIER = BigDecimal.valueOf(1.5);

    private final TransactionRepository transactionRepository;

    public LimitAndAmountValidation(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Recupera o histórico recente de transações do cartão.
     * Limitar o volume evita ruído e melhora performance.
     */
    private List<Transaction> getLastTransactions(Card card) {
        return transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);
    }

    /**
     * Calcula a média de valores das transações históricas.
     * Usado como baseline comportamental.
     */
    private BigDecimal calculateAverage(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
                BigDecimal.valueOf(transactions.size()),
                SCALE,
                RoundingMode.HALF_UP
        );
    }

    /**
     * =========================
     * HIGH_AMOUNT
     * =========================
     *
     * Detecta valores significativamente acima da média histórica.
     * Não dispara por pequenas variações — usa multiplicador.
     *
     * Peso: 20
     * Forte quando combinado com device ou localização anômala.
     */
    public Integer highAmountValidation(Transaction transaction) {
        Card card = transaction.getCard();
        List<Transaction> transactions = getLastTransactions(card);

        // Histórico insuficiente → risco neutro
        if (transactions.size() < 5) {
            return 0;
        }

        BigDecimal avg = calculateAverage(transactions);

        BigDecimal threshold = avg.multiply(HIGH_AMOUNT_MULTIPLIER);

        if (transaction.getAmount().compareTo(threshold) > 0) {
            return AlertType.HIGH_AMOUNT.getScore();
        }

        return 0;
    }

    /**
     * =========================
     * LIMIT_EXCEEDED
     * =========================
     *
     * Verifica se a transação ultrapassa o limite disponível do cartão.
     * Não é fraude isoladamente, mas indica risco operacional alto.
     *
     * Peso: 40
     */
    public Integer limitExceededValidation(Transaction transaction) {
        Card card = transaction.getCard();
        List<Transaction> transactions = getLastTransactions(card);

        // Soma apenas valores já comprometidos
        BigDecimal usedLimit = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableLimit = card.getCreditLimit().subtract(usedLimit);

        if (transaction.getAmount().compareTo(availableLimit) > 0) {
            return AlertType.LIMIT_EXCEEDED.getScore();
        }

        return 0;
    }
}
