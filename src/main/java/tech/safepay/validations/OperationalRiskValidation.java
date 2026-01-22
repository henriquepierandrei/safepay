package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;
import java.util.List;

@Component
public class OperationalRiskValidation {
    /**
     * MULTIPLE_FAILED_ATTEMPTS
     */
    public ValidationResultDto multipleFailedAttempts(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        List<Transaction> recentTransactions = snapshot.last5Minutes();

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
     */
    public ValidationResultDto suspiciousSuccessAfterFailure(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        if (transaction.getTransactionStatus() != TransactionStatus.APPROVED) {
            return result;
        }

        List<Transaction> lastTransactions = snapshot.last20();

        long failedBeforeApproval = lastTransactions.stream()
                .skip(1) // ignora a atual
                .limit(4) // olha só as imediatamente anteriores
                .filter(t -> t.getTransactionStatus() == TransactionStatus.NOT_APPROVED)
                .count();

        if (failedBeforeApproval >= 2) {
            result.addScore(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE.getScore());
            result.addAlert(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE);
        }

        return result;
    }
}
