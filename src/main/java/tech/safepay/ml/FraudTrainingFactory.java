package tech.safepay.ml;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Transaction;

import java.util.List;

@Component
public class FraudTrainingFactory {

    public FraudTraining from(
            Transaction transaction,
            List<AlertType> alerts,
            int totalScore
    ) {

        FraudTraining ft = new FraudTraining();

        ft.setTransactionId(transaction.getTransactionId());
        ft.setRiskScore(totalScore);
        ft.setAlertCount(alerts.size());

        ft.setMaxAlertScore(
                alerts.stream()
                        .mapToInt(AlertType::getScore)
                        .max()
                        .orElse(0)
        );

        // ONE-HOT dos alertas
        ft.setHighAmount(alerts.contains(AlertType.HIGH_AMOUNT));
        ft.setLimitExceeded(alerts.contains(AlertType.LIMIT_EXCEEDED));
        ft.setVelocityAbuse(alerts.contains(AlertType.VELOCITY_ABUSE));
        ft.setBurstActivity(alerts.contains(AlertType.BURST_ACTIVITY));
        ft.setLocationAnomaly(alerts.contains(AlertType.LOCATION_ANOMALY));
        ft.setImpossibleTravel(alerts.contains(AlertType.IMPOSSIBLE_TRAVEL));
        ft.setHighRiskCountry(alerts.contains(AlertType.HIGH_RISK_COUNTRY));
        ft.setNewDeviceDetected(alerts.contains(AlertType.NEW_DEVICE_DETECTED));
        ft.setDeviceFingerprintChange(alerts.contains(AlertType.DEVICE_FINGERPRINT_CHANGE));
        ft.setTorOrProxyDetected(alerts.contains(AlertType.TOR_OR_PROXY_DETECTED));
        ft.setMultipleCardsSameDevice(alerts.contains(AlertType.MULTIPLE_CARDS_SAME_DEVICE));
        ft.setTimeOfDayAnomaly(alerts.contains(AlertType.TIME_OF_DAY_ANOMALY));
        ft.setCardTesting(alerts.contains(AlertType.CARD_TESTING));
        ft.setMicroTransactionPattern(alerts.contains(AlertType.MICRO_TRANSACTION_PATTERN));
        ft.setDeclineThenApprovePattern(alerts.contains(AlertType.DECLINE_THEN_APPROVE_PATTERN));
        ft.setMultipleFailedAttempts(alerts.contains(AlertType.MULTIPLE_FAILED_ATTEMPTS));
        ft.setSuspiciousSuccessAfterFailure(alerts.contains(AlertType.SUSPICIOUS_SUCCESS_AFTER_FAILURE));
        ft.setAnomalyModelTriggered(alerts.contains(AlertType.ANOMALY_MODEL_TRIGGERED));
        ft.setCreditLimitReached(alerts.contains(AlertType.CREDIT_LIMIT_REACHED));
        ft.setExpirationDateApproaching(alerts.contains(AlertType.EXPIRATION_DATE_APPROACHING));

        // LABEL (o que o modelo vai aprender)
        ft.setFinalDecision(transaction.getTransactionDecision());

        return ft;
    }
}
