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

/**
 * Componente responsável por validações relacionadas a limites e valores de transações.
 * <p>
 * Esta classe implementa mecanismos de detecção de anomalias baseadas em análise
 * quantitativa dos valores transacionados, comparando com padrões históricos e
 * limites disponíveis do cartão.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de valores altos atípicos (HIGH_AMOUNT)</li>
 *   <li>Detecção de excesso de limite (LIMIT_EXCEEDED)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class LimitAndAmountValidation {

    /**
     * Escala padrão para operações com BigDecimal (casas decimais).
     */
    private static final int SCALE = 2;

    /**
     * Multiplicador de tolerância para considerar um valor significativamente acima da média.
     * <p>
     * Valor de 1.5 significa que transações acima de 150% da média histórica
     * serão sinalizadas como atípicas.
     * </p>
     */
    private static final BigDecimal HIGH_AMOUNT_MULTIPLIER = BigDecimal.valueOf(1.5);

    /**
     * Calcula a média aritmética dos valores de um conjunto de transações.
     * <p>
     * Este método é utilizado para estabelecer o baseline comportamental
     * do cartão, permitindo identificar desvios significativos no padrão
     * de gastos do usuário.
     * </p>
     *
     * @param transactions lista de transações históricas para cálculo
     * @return média dos valores transacionados com precisão de 2 casas decimais,
     *         ou {@link BigDecimal#ZERO} se a lista estiver vazia
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
     * Valida se o valor da transação está significativamente acima do padrão histórico.
     * <p>
     * Esta validação detecta transações cujo valor esteja substancialmente acima
     * do comportamento histórico do cartão, indicando possível uso fraudulento
     * ou atípico do cartão.
     * </p>
     * <p>
     * <strong>Racional:</strong>
     * <br>
     * Valores fora do padrão individual do cliente tendem a ocorrer em cenários
     * de fraude oportunista ou uso indevido pontual, especialmente quando
     * combinados com device ou localização atípicos.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera histórico recente do cartão (últimas 20 transações)</li>
     *   <li>Calcula a média dos valores transacionados</li>
     *   <li>Aplica multiplicador de tolerância (1.5x) para evitar falsos positivos</li>
     *   <li>Compara o valor atual com o threshold calculado</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Regra de Ativação:</strong>
     * <br>
     * {@code valor_atual > média_histórica × 1.5}
     * </p>
     * <p>
     * <strong>Critérios Mínimos:</strong>
     * <ul>
     *   <li>Mínimo de 5 transações no histórico para baseline confiável</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 20 pontos
     * <br>
     * Não deve bloquear isoladamente. Atua como sinal de reforço no score global.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se houver menos de 5 transações no histórico
     *         ou se o valor não exceder o threshold calculado.
     * @see AlertType#HIGH_AMOUNT
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     */
    public ValidationResultDto highAmountValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        List<Transaction> transactions = snapshot.last20();

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
     * Valida se a transação excede o limite de crédito disponível do cartão.
     * <p>
     * Esta validação identifica tentativas de transação acima do limite disponível
     * real do cartão no momento da autorização, considerando o saldo comprometido
     * em transações recentes.
     * </p>
     * <p>
     * <strong>Racional:</strong>
     * <br>
     * Embora não seja fraude por definição, esse comportamento indica risco
     * operacional elevado e frequentemente aparece associado a tentativas
     * automatizadas ou uso abusivo do cartão.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Soma os valores já comprometidos no histórico recente (últimas 20 transações)</li>
     *   <li>Calcula o limite disponível efetivo: {@code limite_total - limite_usado}</li>
     *   <li>Compara com o valor da transação atual</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Regra de Ativação:</strong>
     * <br>
     * {@code valor_atual > limite_disponível}
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 40 pontos
     * <br>
     * Pode acionar bloqueios ou revisões adicionais devido ao alto risco operacional.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se o valor da transação não exceder o limite disponível.
     * @see AlertType#LIMIT_EXCEEDED
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     * @see Card#getCreditLimit()
     */
    public ValidationResultDto limitExceededValidation(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        List<Transaction> transactions = snapshot.last20();

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