package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OperationalRiskValidation {

    private final TransactionRepository transactionRepository;

    public OperationalRiskValidation(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * MULTIPLE_FAILED_ATTEMPTS
     *
     * Objetivo:
     * Detectar múltiplas tentativas falhas consecutivas em curto intervalo de tempo.
     *
     * Contexto de fraude:
     * Muito comum em ataques de brute force, card testing avançado ou
     * tentativa de descoberta de CVV / data de validade.
     *
     * Estratégia:
     * - Analisa transações recentes do mesmo cartão
     * - Considera apenas transações recusadas
     *
     * Regra:
     * - 3 ou mais falhas consecutivas em janela curta → sinal de risco
     *
     * Observações:
     * - Não bloqueia sozinho
     * - Ganha força quando combinado com velocity, VPN ou fingerprint change
     * - Peso médio (25)
     */
    public ValidationResultDto multipleFailedAttempts(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();
        Card card = transaction.getCard();
        if (card == null) return result;

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(5);
        List<Transaction> recentTransactions =
                transactionRepository.findByCardAndCreatedAtAfter(card, windowStart);

        long failedCount = recentTransactions.stream()
                .filter(t -> t.getTransactionStatus() == TransactionStatus.NOT_APPROVED)
                .count();

        if (failedCount >= 3) {
            result.addScore(AlertType.MULTIPLE_FAILED_ATTEMPTS.getScore());
            result.addAlert(AlertType.MULTIPLE_FAILED_ATTEMPTS);
        }
        return result;
    }


    /**
     * SUSPICIOUS_SUCCESS_AFTER_FAILURE
     *
     * Objetivo:
     * Identificar aprovação suspeita após sequência de falhas.
     *
     * Contexto de fraude:
     * Padrão clássico de ataque bem-sucedido:
     * - Fraudador testa dados incorretos
     * - Ajusta parâmetros
     * - Consegue uma aprovação válida
     *
     * Estratégia:
     * - Recupera as últimas transações do cartão
     * - Verifica se houve falhas antes da aprovação atual
     *
     * Regra:
     * - Transação atual aprovada
     * - Pelo menos 2 falhas imediatamente anteriores
     *
     * Observações:
     * - Altíssimo valor preditivo
     * - Deve elevar score global rapidamente
     * - Peso alto (35)
     */
    public ValidationResultDto suspiciousSuccessAfterFailure(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();
        Card card = transaction.getCard();
        if (card == null || transaction.getTransactionStatus() != TransactionStatus.APPROVED) return result;

        List<Transaction> lastTransactions =
                transactionRepository.findTop5ByCardOrderByCreatedAtDesc(card);

        long failedBeforeApproval = lastTransactions.stream()
                .skip(1) // ignora a transação atual
                .filter(t -> t.getTransactionStatus() == TransactionStatus.NOT_APPROVED)
                .count();

        if (failedBeforeApproval >= 2) {
            result.addScore(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE.getScore());
            result.addAlert(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE);
        }

        return result;
    }

}
