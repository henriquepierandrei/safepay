package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class FraudPatternsValidation {

    private final TransactionRepository transactionRepository;

    public FraudPatternsValidation(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

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
    public int cardTestingPattern(Transaction transaction) {

        Card card = transaction.getCard();

        if (card == null) {
            return 0;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(10);

        List<Transaction> recentTransactions =
                transactionRepository.findByCardAndCreatedAtAfter(card, windowStart);

        long veryLowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        long lowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(5)) <= 0)
                .count();

        // Card testing agressivo (quase determinístico)
        if (veryLowValueCount >= 3) {
            return AlertType.CARD_TESTING.getScore();
        }

        // Card testing clássico
        if (lowValueCount >= 5) {
            return AlertType.CARD_TESTING.getScore();
        }

        return 0;
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
    public int microTransactionPattern(Transaction transaction) {

        Card card = transaction.getCard();

        if (card == null) {
            return 0;
        }

        List<Transaction> lastTransactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        if (lastTransactions.size() < 5) {
            return 0;
        }

        long microCount = lastTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        // Exemplo: mais de 60% das últimas transações são microvalores
        double ratio = (double) microCount / lastTransactions.size();

        return ratio >= 0.6
                ? AlertType.MICRO_TRANSACTION_PATTERN.getScore()
                : 0;
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
    public int declineThenApprovePattern(Transaction transaction) {

        Card card = transaction.getCard();

        if (card == null || Boolean.FALSE.equals(transaction.getApproved())) {
            return 0;
        }

        List<Transaction> lastTransactions =
                transactionRepository.findTop10ByCardOrderByCreatedAtDesc(card);

        if (lastTransactions.size() < 4) {
            return 0;
        }

        long declinedCount = lastTransactions.stream()
                .skip(1) // ignora a transação atual
                .limit(3)
                .filter(t -> Boolean.FALSE.equals(t.getApproved()))
                .count();

        // Exemplo: 3 recusas seguidas antes de uma aprovação
        return declinedCount >= 3
                ? AlertType.DECLINE_THEN_APPROVE_PATTERN.getScore()
                : 0;
    }
}
