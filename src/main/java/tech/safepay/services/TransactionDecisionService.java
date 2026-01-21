package tech.safepay.services;

import org.springframework.stereotype.Service;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;
import tech.safepay.validations.TransactionGlobalValidation;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionDecisionService {

    private final TransactionGlobalValidation validation;
    private final FraudAlertFactory fraudAlertFactory;

    public TransactionDecisionService(
            TransactionGlobalValidation validation,
            FraudAlertFactory fraudAlertFactory
    ) {
        this.validation = validation;
        this.fraudAlertFactory = fraudAlertFactory;
    }

    /**
     * Resultado interno da decisão antifraude
     * NÃO é DTO de API
     */
    public record DecisionResult(
            Transaction transaction,
            List<FraudAlert> alerts,
            int fraudScore
    ) {}

    /**
     * Executa decisão antifraude:
     * - Calcula score
     * - Define flags
     * - Gera lista de alertas
     */
    public ValidationResultDto evaluate(Transaction transaction) {

        // 1️⃣ Executa todas as validações e coleta resultado único
        ValidationResultDto result = validation.validateAll(transaction);

        // 2️⃣ Pega score e alertas diretamente (já vem em result)
        int totalScore = result.getScore();
        List<AlertType> triggeredAlerts = result.getTriggeredAlerts();

        // 3️⃣ Define status da transação baseado no score
        if (totalScore < 30) {
            transaction.setTransactionStatus(TransactionStatus.APPROVED);
            transaction.setFraud(false);
        } else if (totalScore >= 70) {
            transaction.setTransactionStatus(TransactionStatus.NOT_APPROVED);
            transaction.setFraud(true);
        }

        // 4️⃣ Cria um único FraudAlert com todos os alert types (opcional: salvar depois)
        if (!triggeredAlerts.isEmpty()) {
            FraudAlert alert = fraudAlertFactory.create(transaction, triggeredAlerts, totalScore);
        }

        // 5️⃣ Retorna ValidationResultDto consolidado
        return new ValidationResultDto(totalScore, triggeredAlerts);
    }




}
