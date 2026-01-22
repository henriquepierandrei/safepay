package tech.safepay.validations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class TransactionGlobalValidation {

    private static final Logger log = LoggerFactory.getLogger(TransactionGlobalValidation.class);

    private final ExternalAntifraudModelSimulation externalAntifraudModelSimulation;
    private final FraudPatternsValidation fraudPatternsValidation;
    private final FrequencyAndVelocityValidation frequencyAndVelocityValidation;
    private final LimitAndAmountValidation limitAndAmountValidation;
    private final LocationValidation locationValidation;
    private final NetworkAndDeviceValidation networkAndDeviceValidation;
    private final OperationalRiskValidation operationalRiskValidation;
    private final UserBehaviorValidation userBehaviorValidation;
    private final LimitAndExpirationValidation limitAndExpirationValidation;

    private final ValidationContext context;
    private final Executor validationExecutor;

    public TransactionGlobalValidation(
            ValidationContext context,
            ExternalAntifraudModelSimulation externalAntifraudModelSimulation,
            FraudPatternsValidation fraudPatternsValidation,
            FrequencyAndVelocityValidation frequencyAndVelocityValidation,
            LimitAndAmountValidation limitAndAmountValidation,
            LocationValidation locationValidation,
            NetworkAndDeviceValidation networkAndDeviceValidation,
            OperationalRiskValidation operationalRiskValidation,
            UserBehaviorValidation userBehaviorValidation,
            LimitAndExpirationValidation limitAndExpirationValidation,
            @Qualifier("validationExecutor") Executor validationExecutor
    ) {
        this.context = context;
        this.externalAntifraudModelSimulation = externalAntifraudModelSimulation;
        this.fraudPatternsValidation = fraudPatternsValidation;
        this.frequencyAndVelocityValidation = frequencyAndVelocityValidation;
        this.limitAndAmountValidation = limitAndAmountValidation;
        this.locationValidation = locationValidation;
        this.networkAndDeviceValidation = networkAndDeviceValidation;
        this.operationalRiskValidation = operationalRiskValidation;
        this.userBehaviorValidation = userBehaviorValidation;
        this.limitAndExpirationValidation = limitAndExpirationValidation;
        this.validationExecutor = validationExecutor;
    }

    // 📦 Snapshot IMUTÁVEL (async-safe)
    public record ValidationSnapshot(
            List<Transaction> last20,
            List<Transaction> last10,
            List<Transaction> last24Hours,
            List<Transaction> last10Minutes,
            List<Transaction> last5Minutes
    ) {}


    public ValidationResultDto validateAll(Transaction transaction) {

        // 1️⃣ Carrega contexto NA THREAD HTTP
        context.loadContext(transaction);

        ValidationSnapshot snapshot = new ValidationSnapshot(
                context.getLast20Transactions(),
                context.getLast10Transactions(),
                context.getLast24Hours(),
                context.getLast10Minutes(),
                context.getLast5Minutes()
        );

        ValidationResultDto finalResult = new ValidationResultDto();

        // 2️⃣ Execução paralela (SEM RequestScope)
        List<CompletableFuture<ValidationResultDto>> futures = List.of(

                CompletableFuture.supplyAsync(
                        () -> externalAntifraudModelSimulation.anomalyModelTriggered(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.cardTestingPattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.microTransactionPattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> frequencyAndVelocityValidation.velocityAbuseValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> frequencyAndVelocityValidation.burstActivityValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.declineThenApprovePattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndAmountValidation.highAmountValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndAmountValidation.limitExceededValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.impossibleTravelValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.highRiskCountryValidation(transaction),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.locationAnomalyValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.newDeviceDetected(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.deviceFingerprintChange(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.torOrProxyDetected(transaction),
                        validationExecutor
                ),
                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.multipleCardsSameDevice(transaction),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> operationalRiskValidation.multipleFailedAttempts(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> operationalRiskValidation.suspiciousSuccessAfterFailure(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> userBehaviorValidation.timeOfDayAnomaly(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndExpirationValidation.validate(transaction),
                        validationExecutor
                )
        );

        // 3️⃣ Aguarda tudo
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 4️⃣ Consolidação
        futures.stream()
                .map(CompletableFuture::join)
                .forEach(r -> {
                    finalResult.addScore(r.getScore());
                    r.getTriggeredAlerts().forEach(finalResult::addAlert);
                });

        return finalResult;
    }
}
