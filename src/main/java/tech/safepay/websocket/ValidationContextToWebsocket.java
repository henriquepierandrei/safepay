package tech.safepay.websocket;
import org.springframework.stereotype.Component;
import tech.safepay.repositories.TransactionRepository;
import tech.safepay.validations.ValidationContext;

@Component
public class ValidationContextToWebsocket {

    private final TransactionRepository transactionRepository;

    public ValidationContextToWebsocket(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

//    public ValidationContext create() {
//        // Cria manualmente uma instância
//        ValidationContext context = new ValidationContext(transactionRepository);
//
//        // Popula as listas que normalmente seriam carregadas por request
//        context.loadAllTransactions();
//
//        return context;
//    }
}
