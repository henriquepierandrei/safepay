package tech.safepay.services;

import org.springframework.stereotype.Service;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.validations.TransactionGlobalValidation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionDecisionService {

    private final TransactionGlobalValidation validation;
    private final CardPatternService cardPatternService;
    private final FraudAlertFactory fraudAlertFactory;
    private final CardRepository cardRepository;





    public TransactionDecisionService(TransactionGlobalValidation validation, CardPatternService cardPatternService, FraudAlertFactory fraudAlertFactory,
                                      CardRepository cardRepository) {
        this.validation = validation;
        this.cardPatternService = cardPatternService;
        this.fraudAlertFactory = fraudAlertFactory;
        this.cardRepository = cardRepository;
    }

    /**
     * Resultado interno da decisão antifraude
     * NÃO é DTO de API
     */
    public record DecisionResult(
            Transaction transaction,
            List<FraudAlert> alerts,
            int fraudScore
    ) {
    }

    /**
     * Executa decisão antifraude:
     * - Calcula score
     * - Define flags
     * - Gera lista de alertas
     */
    public ValidationResultDto evaluate(Transaction transaction, boolean successForce) {

        // 1️⃣ Executa todas as validações e coleta resultado único
        ValidationResultDto result = validation.validateAll(transaction);

        // 2️⃣ Pega score e alertas diretamente
        int totalScore = result.getScore();
        List<AlertType> triggeredAlerts = result.getTriggeredAlerts();

        // 3️⃣ Define status da transação baseado no score
        if (totalScore < 25) {
            transaction.setTransactionDecision(TransactionDecision.APPROVED);
            transaction.setFraud(false);
        } else if (totalScore >= 25 && totalScore < 60) {
            transaction.setTransactionDecision(TransactionDecision.REVIEW);
            transaction.setFraud(false);
        } else { // totalScore >= 60
            transaction.setTransactionDecision(TransactionDecision.BLOCKED);
            transaction.setFraud(true);
        }

        // 4️⃣ Override se for teste forçado
        if (successForce) {
            transaction.setTransactionDecision(TransactionDecision.APPROVED);
        }

        if (result.getTriggeredAlerts().contains(AlertType.CREDIT_LIMIT_REACHED)) {
            transaction.setTransactionDecision(TransactionDecision.BLOCKED);
        }


        // 5️⃣ Cria FraudAlert se houver alertas
        if (!triggeredAlerts.isEmpty()) {

            FraudAlert alert = fraudAlertFactory.create(
                    transaction,
                    triggeredAlerts,
                    totalScore
            );

        }


        cardPatternService.buildOrUpdateCardPattern(transaction.getCard());

        if (transaction.getTransactionDecision() == TransactionDecision.APPROVED) {
            var card = transaction.getCard();

            BigDecimal remaining = card.getRemainingLimit();
            if (remaining == null) remaining = card.getCreditLimit() != null ? card.getCreditLimit() : BigDecimal.ZERO;

            card.setRemainingLimit(remaining.subtract(transaction.getAmount()));
            card.setLastTransactionAt(LocalDateTime.now());

             cardRepository.save(card);
        }

        // 6️⃣ Retorna ValidationResultDto consolidado
        return new ValidationResultDto(totalScore, triggeredAlerts);

    }
}
