package tech.safepay.validations;

import org.springframework.stereotype.Component;
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

    public TransactionGlobalValidation(
            ExternalAntifraudModelSimulation externalAntifraudModelSimulation,
            FraudPatternsValidation fraudPatternsValidation,
            FrequencyAndVelocityValidation frequencyAndVelocityValidation,
            LimitAndAmountValidation limitAndAmountValidation,
            LocalizationValidation localizationValidation,
            NetworkAndDeviceValidation networkAndDeviceValidation,
            OperationalRiskValidation operationalRiskValidation,
            UserBehaviorValidation userBehaviorValidation
    ) {
        this.externalAntifraudModelSimulation = externalAntifraudModelSimulation;
        this.fraudPatternsValidation = fraudPatternsValidation;
        this.frequencyAndVelocityValidation = frequencyAndVelocityValidation;
        this.limitAndAmountValidation = limitAndAmountValidation;
        this.localizationValidation = localizationValidation;
        this.networkAndDeviceValidation = networkAndDeviceValidation;
        this.operationalRiskValidation = operationalRiskValidation;
        this.userBehaviorValidation = userBehaviorValidation;
    }

    /**
     * Executa todas as validações antifraude e retorna
     * o score agregado da transação.
     *
     * Fluxo:
     * 1. Modelo externo (simulado) de anomalia
     * 2. Padrões clássicos de fraude
     * 3. Frequência e velocidade
     * 4. Valor e limite
     * 5. Localização geográfica
     * 6. Dispositivo e rede
     * 7. Risco operacional
     * 8. Comportamento do usuário
     *
     * Retorno:
     * - Inteiro representando o risco total da transação
     */
    public Integer validateAll(Transaction transaction) {

        int fraudScore = 0;

        // =========================
        // MODELO EXTERNO (SIMULADO)
        // =========================
        fraudScore += externalAntifraudModelSimulation
                .anomalyModelTriggered(transaction);

        // =========================
        // PADRÕES CLÁSSICOS DE FRAUDE
        // =========================
        fraudScore += fraudPatternsValidation
                .cardTestingPattern(transaction);

        fraudScore += fraudPatternsValidation
                .microTransactionPattern(transaction);

        fraudScore += fraudPatternsValidation
                .declineThenApprovePattern(transaction);

        // =========================
        // FREQUÊNCIA & VELOCIDADE
        // =========================
        fraudScore += frequencyAndVelocityValidation
                .velocityAbuseValidation(transaction);

        fraudScore += frequencyAndVelocityValidation
                .burstActivityValidation(transaction);

        // =========================
        // VALOR & LIMITE
        // =========================
        fraudScore += limitAndAmountValidation
                .highAmountValidation(transaction);

        fraudScore += limitAndAmountValidation
                .limitExceededValidation(transaction);

        // =========================
        // LOCALIZAÇÃO
        // =========================
        fraudScore += localizationValidation
                .impossibleTravelValidation(transaction);

        fraudScore += localizationValidation
                .locationAnomalyValidation(transaction);

        fraudScore += localizationValidation
                .highRiskCountryValidation(transaction);

        // =========================
        // DISPOSITIVO & REDE
        // =========================
        fraudScore += networkAndDeviceValidation
                .deviceFingerprintChange(transaction);

        fraudScore += networkAndDeviceValidation
                .newDeviceDetected(transaction);

        fraudScore += networkAndDeviceValidation
                .multipleCardsSameDevice(transaction);

        fraudScore += networkAndDeviceValidation
                .torOrProxyDetected(transaction);

        // =========================
        // RISCO OPERACIONAL
        // =========================
        fraudScore += operationalRiskValidation
                .multipleFailedAttempts(transaction);

        fraudScore += operationalRiskValidation
                .suspiciousSuccessAfterFailure(transaction);

        // =========================
        // COMPORTAMENTO DO USUÁRIO
        // =========================
        fraudScore += userBehaviorValidation
                .timeOfDayAnomaly(transaction);

        return fraudScore;
    }
}
