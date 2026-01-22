package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class LimitAndExpirationValidation {

    /**
     * Valida se a transação ultrapassa o limite do cartão
     * e se o cartão está próximo da data de expiração.
     */
    public ValidationResultDto validate(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        BigDecimal transactionAmount = transaction.getAmount();
        LocalDate expirationDate = card.getExpirationDate();

        // =====================
        // 1️⃣ Valida limite de crédito
        // =====================
        if (transactionAmount.compareTo(card.getRemainingLimit()) > 0) {
            result.addScore(AlertType.CREDIT_LIMIT_REACHED.getScore());
            result.addAlert(AlertType.CREDIT_LIMIT_REACHED);
        }

        // =====================
        // 2️⃣ Valida proximidade da expiração (30 dias)
        // =====================
        long daysToExpiration = LocalDate.now().until(expirationDate, ChronoUnit.DAYS);
        if (daysToExpiration <= 30) {
            result.addScore(AlertType.EXPIRATION_DATE_APPROACHING.getScore());
            result.addAlert(AlertType.EXPIRATION_DATE_APPROACHING);
        }

        return result;
    }

}
