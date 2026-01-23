package tech.safepay.validations;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)     // NÃO REMOVER, SE REMOVER O ALGORITMO DE VALIDAÇÃO QUEBRA!
public class ValidationContext {

    private final TransactionRepository transactionRepository;
    private boolean loaded = false;

    private List<Transaction> last20Transactions = Collections.emptyList();
    private List<Transaction> last10Transactions = Collections.emptyList();
    private List<Transaction> last24HoursTransactions = Collections.emptyList();
    private List<Transaction> last5MinutesTransactions = Collections.emptyList();
    private List<Transaction> last10MinutesTransactions = Collections.emptyList();

    public ValidationContext(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void loadContext(Transaction transaction) {
        if (loaded) return;

        if (transaction == null || transaction.getCard() == null) {
            loaded = true;
            return;
        }

        Card card = transaction.getCard();

        LocalDateTime referenceTime =
                transaction.getCreatedAt() != null
                        ? transaction.getCreatedAt()
                        : LocalDateTime.now();

        // QUERY ÚNICA
        last20Transactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        // DERIVAÇÕES EM MEMÓRIA
        last10Transactions =
                last20Transactions.stream().limit(10).toList();

        last24HoursTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusHours(24)))
                .toList();

        last5MinutesTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusMinutes(5)))
                .toList();

        last10MinutesTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusMinutes(10)))
                .toList();

        loaded = true;
    }

    // GETTERS = ZERO LÓGICA
    public List<Transaction> getLast20Transactions() {
        return last20Transactions;
    }

    public List<Transaction> getLast10Transactions() {
        return last10Transactions;
    }

    public List<Transaction> getLast24Hours() {
        return last24HoursTransactions;
    }

    public List<Transaction> getLast5Minutes() {
        return last5MinutesTransactions;
    }

    public List<Transaction> getLast10Minutes() {
        return last10MinutesTransactions;
    }
}
