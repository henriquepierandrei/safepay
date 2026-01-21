package tech.safepay.services;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;
import tech.safepay.generator.transactions.TransactionGenerator;
import tech.safepay.repositories.FraudAlertRepository;
import tech.safepay.repositories.TransactionRepository;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionPipelineService {

    private final TransactionGenerator transactionGenerator;
    private final TransactionDecisionService decisionService;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertFactory fraudAlertFactory;

    public TransactionPipelineService(TransactionGenerator transactionGenerator, TransactionDecisionService decisionService, TransactionRepository transactionRepository, FraudAlertRepository fraudAlertRepository, FraudAlertFactory fraudAlertFactory) {
        this.transactionGenerator = transactionGenerator;
        this.decisionService = decisionService;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudAlertFactory = fraudAlertFactory;
    }

    /**
     * Pipeline completo:
     * 1. Gera transação
     * 2. Executa antifraude
     * 3. Persiste decisão
     * 4. Persiste alertas
     * 5. Retorna DTO seguro
     */
    @Transactional
    public TransactionPipelineService.TransactionDecisionResponse process( boolean isManual,
                                                                           @Nullable ManualTransactionDto manualTransactionDto
    ) {

        Transaction transaction;

        if (isManual) {
            if (manualTransactionDto == null) {
                throw new IllegalArgumentException(
                        "ManualTransactionDto must be provided for manual processing"
                );
            }
            transaction = transactionGenerator.generateManualTransaction(manualTransactionDto);
        } else {
            transaction = transactionGenerator.generateNormalTransaction();
        }


        // 2️⃣ Avaliação antifraude (retorna ValidationResultDto)
        ValidationResultDto validationResult = decisionService.evaluate(transaction);

        // 3️⃣ Define status da transação já dentro do evaluate
        transactionRepository.save(transaction);

        // 4️⃣ Criação de um único FraudAlert com todos os alert types
        FraudAlert alert = null;
        if (!validationResult.getTriggeredAlerts().isEmpty()) {
            alert = fraudAlertFactory.create(
                    transaction,
                    validationResult.getTriggeredAlerts(),
                    validationResult.getScore()
            );
            fraudAlertRepository.save(alert);
        }

        // 5️⃣ Retorno DTO para API
        return new TransactionDecisionResponse(
                transaction.getTransactionId(),
                transaction.getTransactionStatus(),
                transaction.getFraud(),
                validationResult.getScore(),
                alert == null ? List.of() : alert.getAlertTypes() // lista única
        );
    }


    /**
     * DTO FINAL
     */
    public record TransactionDecisionResponse(
            UUID transactionId,
            TransactionStatus transactionStatus,
            boolean fraud,
            int fraudScore,
            List<AlertType> alertTypes
    ) {}
}
