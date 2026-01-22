package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

/**
 * =========================
 * TRANSACTION GLOBAL VALIDATION
 * =========================
 *
 * Responsabilidade:
 * Orquestrar todas as validações antifraude e consolidar
 * os sinais individuais em um único score de risco.
 *
 * Modelo de decisão:
 * - Cada validação retorna um score ponderado
 * - O score final é a soma de todos os sinais disparados
 * - Nenhuma regra decide isoladamente (exceto casos extremos)
 *
 * Observação importante:
 * Este componente NÃO decide aprovação ou negação.
 * Ele apenas produz um risco agregado para consumo
 * por camadas superiores (policy engine / decision engine).
 */
@Component
public class TransactionGlobalValidation {

    private final ExternalAntifraudModelSimulation externalAntifraudModelSimulation;
    private final FraudPatternsValidation fraudPatternsValidation;
    private final FrequencyAndVelocityValidation frequencyAndVelocityValidation;
    private final LimitAndAmountValidation limitAndAmountValidation;
    private final LocalizationValidation localizationValidation;
    private final NetworkAndDeviceValidation networkAndDeviceValidation;
    private final OperationalRiskValidation operationalRiskValidation;
    private final UserBehaviorValidation userBehaviorValidation;
    private final LimitAndExpirationValidation limitAndExpirationValidation;

    public TransactionGlobalValidation(
            ExternalAntifraudModelSimulation externalAntifraudModelSimulation,
            FraudPatternsValidation fraudPatternsValidation,
            FrequencyAndVelocityValidation frequencyAndVelocityValidation,
            LimitAndAmountValidation limitAndAmountValidation,
            LocalizationValidation localizationValidation,
            NetworkAndDeviceValidation networkAndDeviceValidation,
            OperationalRiskValidation operationalRiskValidation,
            UserBehaviorValidation userBehaviorValidation,
            LimitAndExpirationValidation limitAndExpirationValidation
    ) {
        this.externalAntifraudModelSimulation = externalAntifraudModelSimulation;
        this.fraudPatternsValidation = fraudPatternsValidation;
        this.frequencyAndVelocityValidation = frequencyAndVelocityValidation;
        this.limitAndAmountValidation = limitAndAmountValidation;
        this.localizationValidation = localizationValidation;
        this.networkAndDeviceValidation = networkAndDeviceValidation;
        this.operationalRiskValidation = operationalRiskValidation;
        this.userBehaviorValidation = userBehaviorValidation;
        this.limitAndExpirationValidation = limitAndExpirationValidation;
    }

    /**
     * Executa todas as validações antifraude e retorna
     * o score agregado da transação.
     * <p>
     * Fluxo:
     * 1. Modelo externo (simulado) de anomalia
     * 2. Padrões clássicos de fraude
     * 3. Frequência e velocidade
     * 4. Valor e limite
     * 5. Localização geográfica
     * 6. Dispositivo e rede
     * 7. Risco operacional
     * 8. Comportamento do usuário
     * <p>
     * Retorno:
     * - Inteiro representando o risco total da transação
     */
    public ValidationResultDto validateAll(Transaction transaction) {

        ValidationResultDto finalResult = new ValidationResultDto();

        // =========================
        // MODELO EXTERNO (SIMULADO)
        // =========================
        ValidationResultDto external = externalAntifraudModelSimulation.anomalyModelTriggered(transaction);
        finalResult.addScore(external.getScore());
        external.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // PADRÕES CLÁSSICOS DE FRAUDE
        // =========================
        ValidationResultDto cardTesting = fraudPatternsValidation.cardTestingPattern(transaction);
        finalResult.addScore(cardTesting.getScore());
        cardTesting.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto microTxn = fraudPatternsValidation.microTransactionPattern(transaction);
        finalResult.addScore(microTxn.getScore());
        microTxn.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto declineThenApprove = fraudPatternsValidation.declineThenApprovePattern(transaction);
        finalResult.addScore(declineThenApprove.getScore());
        declineThenApprove.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // FREQUÊNCIA & VELOCIDADE
        // =========================
        ValidationResultDto velocity = frequencyAndVelocityValidation.velocityAbuseValidation(transaction);
        finalResult.addScore(velocity.getScore());
        velocity.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto burst = frequencyAndVelocityValidation.burstActivityValidation(transaction);
        finalResult.addScore(burst.getScore());
        burst.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // VALOR & LIMITE
        // =========================
        ValidationResultDto highAmount = limitAndAmountValidation.highAmountValidation(transaction);
        finalResult.addScore(highAmount.getScore());
        highAmount.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto limitExceeded = limitAndAmountValidation.limitExceededValidation(transaction);
        finalResult.addScore(limitExceeded.getScore());
        limitExceeded.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // LOCALIZAÇÃO
        // =========================
        ValidationResultDto impossibleTravel = localizationValidation.impossibleTravelValidation(transaction);
        finalResult.addScore(impossibleTravel.getScore());
        impossibleTravel.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto locationAnomaly = localizationValidation.locationAnomalyValidation(transaction);
        finalResult.addScore(locationAnomaly.getScore());
        locationAnomaly.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto highRiskCountry = localizationValidation.highRiskCountryValidation(transaction);
        finalResult.addScore(highRiskCountry.getScore());
        highRiskCountry.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // DISPOSITIVO & REDE
        // =========================
        ValidationResultDto fingerprintChange = networkAndDeviceValidation.deviceFingerprintChange(transaction);
        finalResult.addScore(fingerprintChange.getScore());
        fingerprintChange.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto newDevice = networkAndDeviceValidation.newDeviceDetected(transaction);
        finalResult.addScore(newDevice.getScore());
        newDevice.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto multipleCards = networkAndDeviceValidation.multipleCardsSameDevice(transaction);
        finalResult.addScore(multipleCards.getScore());
        multipleCards.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto torProxy = networkAndDeviceValidation.torOrProxyDetected(transaction);
        finalResult.addScore(torProxy.getScore());
        torProxy.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // RISCO OPERACIONAL
        // =========================
        ValidationResultDto failedAttempts = operationalRiskValidation.multipleFailedAttempts(transaction);
        finalResult.addScore(failedAttempts.getScore());
        failedAttempts.getTriggeredAlerts().forEach(finalResult::addAlert);

        ValidationResultDto suspiciousSuccess = operationalRiskValidation.suspiciousSuccessAfterFailure(transaction);
        finalResult.addScore(suspiciousSuccess.getScore());
        suspiciousSuccess.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // COMPORTAMENTO DO USUÁRIO
        // =========================
        ValidationResultDto timeOfDay = userBehaviorValidation.timeOfDayAnomaly(transaction);
        finalResult.addScore(timeOfDay.getScore());
        timeOfDay.getTriggeredAlerts().forEach(finalResult::addAlert);

        // =========================
        // LIMITE & EXPIRAÇÃO
        // =========================
        ValidationResultDto limitAndCreditReached = limitAndExpirationValidation.validate(transaction);
        finalResult.addScore(limitAndCreditReached.getScore());
        limitAndCreditReached.getTriggeredAlerts().forEach(finalResult::addAlert);

        return finalResult;
    }
}
