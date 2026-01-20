package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
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
     * Objetivo:
     * Detectar transações cujo valor esteja significativamente acima
     * do comportamento histórico do cartão.
     *
     * Racional:
     * Valores fora do padrão individual do cliente tendem a ocorrer
     * em cenários de fraude oportunista ou uso indevido pontual,
     * especialmente quando combinados com device ou localização atípicos.
     *
     * Estratégia:
     * 1. Recupera histórico recente do cartão (amostra controlada)
     * 2. Calcula a média dos valores transacionados
     * 3. Aplica um multiplicador de tolerância para evitar falsos positivos
     * 4. Compara o valor atual com o threshold calculado
     *
     * Regra:
     * valor_atual > média_histórica × multiplicador
     *
     * Peso: 20
     * Não deve bloquear isoladamente.
     * Atua como sinal de reforço no score global.
     */

    public ValidationResultDto highAmountValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        List<Transaction> transactions = getLastTransactions(card);

        if (transactions.size() < 5) return result;

        BigDecimal avg = calculateAverage(transactions);
        BigDecimal threshold = avg.multiply(HIGH_AMOUNT_MULTIPLIER);

        if (transaction.getAmount().compareTo(threshold) > 0) {
            result.addScore(AlertType.HIGH_AMOUNT.getScore());
            result.addAlert(AlertType.HIGH_AMOUNT);
        }

        return result;
    }

    /**
     * =========================
     * LIMIT_EXCEEDED
     * =========================
     *
     * Objetivo:
     * Identificar tentativas de transação acima do limite disponível
     * real do cartão no momento da autorização.
     *
     * Racional:
     * Embora não seja fraude por definição, esse comportamento indica
     * risco operacional elevado e frequentemente aparece associado
     * a tentativas automatizadas ou uso abusivo do cartão.
     *
     * Estratégia:
     * 1. Soma os valores já comprometidos no histórico recente
     * 2. Calcula o limite disponível efetivo
     * 3. Compara com o valor da transação atual
     *
     * Regra:
     * valor_atual > limite_disponível
     *
     * Peso: 40
     * Pode acionar bloqueios ou revisões adicionais.
     */

    public ValidationResultDto limitExceededValidation(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        List<Transaction> transactions = getLastTransactions(card);

        BigDecimal usedLimit = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableLimit = card.getCreditLimit().subtract(usedLimit);

        if (transaction.getAmount().compareTo(availableLimit) > 0) {
            result.addScore(AlertType.LIMIT_EXCEEDED.getScore());
            result.addAlert(AlertType.LIMIT_EXCEEDED);
        }

        return result;
    }
}
