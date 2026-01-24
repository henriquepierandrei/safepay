package tech.safepay.services;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.safepay.Enums.Severity;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;
import tech.safepay.generator.transactions.TransactionGenerator;
import tech.safepay.repositories.FraudAlertRepository;
import tech.safepay.repositories.TransactionRepository;

import java.util.UUID;

@Service
public class TransactionPipelineService {

    private final TransactionGenerator transactionGenerator;
    private final TransactionDecisionService decisionService;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertFactory fraudAlertFactory;

    public TransactionPipelineService(
            TransactionGenerator transactionGenerator,
            TransactionDecisionService decisionService,
            TransactionRepository transactionRepository,
            FraudAlertRepository fraudAlertRepository,
            FraudAlertFactory fraudAlertFactory
    ) {
        this.transactionGenerator = transactionGenerator;
        this.decisionService = decisionService;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudAlertFactory = fraudAlertFactory;
    }

    /**
     * PIPELINE COMPLETO DE TRANSAÇÃO
     *
     * 1. Gera transação
     * 2. Executa antifraude
     * 3. Persiste transação
     * 4. Cria (opcionalmente) FraudAlert
     * 5. Retorna DTO seguro
     */
    @Transactional
    public TransactionResponseDto process(
            boolean isManual,
            boolean successForce,
            @Nullable ManualTransactionDto manualTransactionDto
    ) {

        // =========================
        // 1️⃣ GERA TRANSAÇÃO
        // =========================
        Transaction transaction;

        if (isManual) {
            if (manualTransactionDto == null) {
                throw new IllegalArgumentException(
                        "ManualTransactionDto must be provided for manual processing"
                );
            }
            transaction = transactionGenerator.generateManualTransaction(
                    manualTransactionDto,
                    successForce
            );
        } else {
            transaction = transactionGenerator.generateNormalTransaction();
        }

        // =========================
        // 2️⃣ AVALIA ANTIFRAUDE
        // =========================
        ValidationResultDto validationResult =
                decisionService.evaluate(transaction, successForce);

        // =========================
        // 3️⃣ PERSISTE TRANSAÇÃO
        // =========================
        transactionRepository.save(transaction);

        // =========================
        // 4️⃣ CRIA ALERTA (SE NECESSÁRIO)
        // =========================
        FraudAlert alert = null;
        Severity severity = Severity.LOW;

        if (!validationResult.getTriggeredAlerts().isEmpty()) {
            alert = fraudAlertFactory.create(
                    transaction,
                    validationResult.getTriggeredAlerts(),
                    validationResult.getScore()
            );
            fraudAlertRepository.save(alert);
            severity = alert.getSeverity();
        }

        // =========================
        // 5️⃣ RETORNO DTO
        // =========================
        return new TransactionResponseDto(
                null,
                transaction.getMerchantCategory(),
                transaction.getAmount(),
                transaction.getTransactionDateAndTime(),
                transaction.getLatitude(),
                transaction.getLongitude(),
                new ResolvedLocalizationDto(
                        transaction.getCountryCode(),
                        transaction.getState(),
                        transaction.getCity()
                ),
                validationResult,
                severity,
                new DeviceListResponseDto.DeviceDto(
                        transaction.getDevice().getId(),
                        transaction.getDevice().getFingerPrintId(),
                        transaction.getDevice().getDeviceType(),
                        transaction.getDevice().getOs(),
                        transaction.getDevice().getBrowser()
                ),
                transaction.getIpAddress(),
                transaction.getTransactionDecision(),
                transaction.getFraud(),
                transaction.getCreatedAt()
        );
    }
}
