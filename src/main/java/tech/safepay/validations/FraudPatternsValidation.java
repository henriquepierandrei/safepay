package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;

@Component
public class FraudPatternsValidation {
    /**
     * =========================
     * CARD_TESTING (50)
     * =========================
     *
     * Objetivo:
     * Detectar tentativas clássicas de "card testing",
     * onde o fraudador executa múltiplas transações
     * de baixo valor em curto intervalo para validar
     * se o cartão está ativo.
     *
     * Estratégia:
     * - Analisa uma janela curta de tempo (ex: 10 minutos)
     * - Busca múltiplas transações com valores baixos
     * - Verifica repetição de padrão
     *
     * Regra:
     * - Número de transações >= threshold
     * - Valores abaixo de um limite mínimo
     *
     * Observações:
     * - Altíssimo valor preditivo
     * - Normalmente combinado com velocity e device signals
     */
    public ValidationResultDto cardTestingPattern(Transaction transaction,TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        List<Transaction> recentTransactions = snapshot.last10Minutes();

        long veryLowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        long lowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(5)) <= 0)
                .count();

        if (veryLowValueCount >= 3 || lowValueCount >= 5) {
            result.addScore(AlertType.CARD_TESTING.getScore());
            result.addAlert(AlertType.CARD_TESTING);
        }

        return result;
    }


    /**
     * =========================
     * MICRO_TRANSACTION_PATTERN (35)
     * =========================
     *
     * Objetivo:
     * Identificar sequência de microtransações consecutivas,
     * geralmente utilizadas para sondar limites e regras
     * antifraude antes de um ataque maior.
     *
     * Diferença para CARD_TESTING:
     * - Menor volume
     * - Valores ainda mais baixos
     * - Pode ocorrer em intervalos um pouco maiores
     *
     * Estratégia:
     * - Analisa as últimas N transações do cartão
     * - Verifica se a maioria possui valor irrisório
     *
     * Regra:
     * - Percentual significativo de microvalores
     *
     * Observações:
     * - Sinal forte, mas abaixo de card testing puro
     */
    public ValidationResultDto microTransactionPattern(Transaction transaction,  TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        List<Transaction> lastTransactions = snapshot.last20();

        if (lastTransactions.size() < 5) return result;

        long microCount = lastTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        double ratio = (double) microCount / lastTransactions.size();

        if (ratio >= 0.6) {
            result.addScore(AlertType.MICRO_TRANSACTION_PATTERN.getScore());
            result.addAlert(AlertType.MICRO_TRANSACTION_PATTERN);
        }

        return result;
    }

    /**
     * =========================
     * DECLINE_THEN_APPROVE_PATTERN (30)
     * =========================
     *
     * Objetivo:
     * Detectar padrão de múltiplas transações recusadas
     * seguidas por uma aprovação bem-sucedida.
     *
     * Contexto típico:
     * - Brute force de CVV, validade ou limite
     * - Ajuste progressivo até "passar"
     *
     * Estratégia:
     * - Analisa as últimas transações do cartão
     * - Busca sequência de recusas antes de uma aprovação
     *
     * Regra:
     * - Pelo menos N recusas seguidas
     * - Última transação aprovada
     *
     * Observações:
     * - Muito comum em ataques automatizados
     * - Forte quando combinado com device ou IP suspeito
     */
    public ValidationResultDto declineThenApprovePattern(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null || transaction.getTransactionStatus() != TransactionStatus.APPROVED) {
            return result;
        }

        List<Transaction> lastTransactions = snapshot.last10();


        if (lastTransactions.size() < 4) return result;

        long declinedCount = lastTransactions.stream()
                .skip(1) // ignora a transação atual
                .limit(3)
                .filter(t -> t.getTransactionStatus() == TransactionStatus.NOT_APPROVED)
                .count();

        if (declinedCount >= 3) {
            result.addScore(AlertType.DECLINE_THEN_APPROVE_PATTERN.getScore());
            result.addAlert(AlertType.DECLINE_THEN_APPROVE_PATTERN);
        }

        return result;
    }

}
