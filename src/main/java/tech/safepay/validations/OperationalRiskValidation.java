package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;
import java.util.List;

/**
 * Componente responsável por validações de risco operacional.
 * <p>
 * Esta classe implementa mecanismos de detecção de padrões operacionais
 * suspeitos relacionados a tentativas de transação, identificando comportamentos
 * que indicam testes sistemáticos de cartão, ataques de força bruta ou
 * tentativas de contornar sistemas de segurança.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de múltiplas tentativas falhadas (MULTIPLE_FAILED_ATTEMPTS)</li>
 *   <li>Detecção de sucesso suspeito após falhas (SUSPICIOUS_SUCCESS_AFTER_FAILURE)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class OperationalRiskValidation {

    /**
     * Threshold mínimo de tentativas falhadas para sinalizar comportamento suspeito.
     */
    private static final long FAILED_ATTEMPTS_THRESHOLD = 3;

    /**
     * Threshold mínimo de falhas antes de aprovação para sinalizar sucesso suspeito.
     */
    private static final long FAILED_BEFORE_SUCCESS_THRESHOLD = 2;

    /**
     * Número de transações anteriores a verificar após sucesso.
     */
    private static final long TRANSACTIONS_TO_CHECK_AFTER_SUCCESS = 4;

    /**
     * Valida se há múltiplas tentativas de transação falhadas em curto período.
     * <p>
     * Esta validação detecta padrões de tentativas repetidas e malsucedidas de
     * realizar transações, comportamento típico de:
     * <ul>
     *   <li>Card testing (teste de validade de cartões roubados)</li>
     *   <li>Ataques de força bruta para descobrir CVV ou outros dados</li>
     *   <li>Bots automatizados testando múltiplos cartões</li>
     *   <li>Tentativas de encontrar limites ou valores aprovados</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera transações dos últimos 5 minutos</li>
     *   <li>Filtra transações com decisão BLOCKED</li>
     *   <li>Conta o número de tentativas bloqueadas</li>
     *   <li>Sinaliza se houver 3 ou mais bloqueios</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Janela de Observação:</strong> 5 minutos
     * <br>
     * <strong>Critério de Ativação:</strong> {@code tentativas_bloqueadas >= 3}
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> definido em {@link AlertType#MULTIPLE_FAILED_ATTEMPTS}
     * <br>
     * Sinal forte de atividade automatizada ou tentativas maliciosas.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico recente de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se o número de tentativas falhadas for inferior
     *         ao threshold.
     * @see AlertType#MULTIPLE_FAILED_ATTEMPTS
     * @see TransactionDecision#BLOCKED
     * @see TransactionGlobalValidation.ValidationSnapshot#last5Minutes()
     */
    public ValidationResultDto multipleFailedAttempts(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> recentTransactions = snapshot.last5Minutes();

        long failedCount = recentTransactions.stream()
                .filter(t -> t.getTransactionDecision() == TransactionDecision.BLOCKED)
                .count();

        if (failedCount >= FAILED_ATTEMPTS_THRESHOLD) {
            result.addScore(AlertType.MULTIPLE_FAILED_ATTEMPTS.getScore());
            result.addAlert(AlertType.MULTIPLE_FAILED_ATTEMPTS);
        }

        return result;
    }

    /**
     * Valida se uma transação aprovada ocorre logo após múltiplas tentativas falhadas.
     * <p>
     * Esta validação detecta o padrão suspeito onde, após várias tentativas bloqueadas,
     * uma transação é subitamente aprovada. Este comportamento pode indicar:
     * <ul>
     *   <li>Fraudador ajustando parâmetros até encontrar combinação aprovada</li>
     *   <li>Teste sistemático de diferentes valores ou dados até contornar bloqueios</li>
     *   <li>Exploração de janela temporal ou limites do sistema</li>
     *   <li>Sucesso após tentativas de card testing</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Verifica se a transação atual foi aprovada</li>
     *   <li>Recupera as últimas 20 transações do histórico</li>
     *   <li>Analisa as 4 transações imediatamente anteriores (ignorando a atual)</li>
     *   <li>Conta quantas foram bloqueadas</li>
     *   <li>Sinaliza se 2 ou mais das 4 anteriores foram bloqueadas</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Condições de Ativação:</strong>
     * <ul>
     *   <li>Transação atual tem decisão APPROVED</li>
     *   <li>2 ou mais das 4 transações anteriores foram BLOCKED</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Condições de NÃO Ativação:</strong>
     * <ul>
     *   <li>Transação atual não foi aprovada</li>
     *   <li>Menos de 2 bloqueios nas transações anteriores</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> definido em {@link AlertType#SUSPICIOUS_SUCCESS_AFTER_FAILURE}
     * <br>
     * Indica possível exploração bem-sucedida após tentativas de contornar segurança.
     * </p>
     *
     * @param transaction a transação atual sendo validada (deve estar aprovada)
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: a transação não foi aprovada ou se não
     *         houver padrão de falhas seguidas de sucesso.
     * @see AlertType#SUSPICIOUS_SUCCESS_AFTER_FAILURE
     * @see TransactionDecision#APPROVED
     * @see TransactionDecision#BLOCKED
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     */
    public ValidationResultDto suspiciousSuccessAfterFailure(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        if (transaction.getTransactionDecision() != TransactionDecision.APPROVED) {
            return result;
        }

        List<Transaction> lastTransactions = snapshot.last20();

        long failedBeforeApproval = lastTransactions.stream()
                .skip(1) // ignora a atual
                .limit(TRANSACTIONS_TO_CHECK_AFTER_SUCCESS) // olha só as imediatamente anteriores
                .filter(t -> t.getTransactionDecision() == TransactionDecision.BLOCKED)
                .count();

        if (failedBeforeApproval >= FAILED_BEFORE_SUCCESS_THRESHOLD) {
            result.addScore(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE.getScore());
            result.addAlert(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE);
        }

        return result;
    }
}