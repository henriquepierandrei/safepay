package tech.safepay.services;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;
import tech.safepay.Enums.Severity;
import tech.safepay.dtos.cards.CardDataResponseDto;
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

/**
 * Serviço responsável pela orquestração completa do pipeline de processamento de transações.
 * <p>
 * Este serviço coordena o fluxo end-to-end de uma transação desde sua criação até
 * a persistência final, incluindo:
 * <ul>
 *   <li>Geração de transação (automática ou manual)</li>
 *   <li>Avaliação antifraude completa</li>
 *   <li>Persistência da transação e alertas</li>
 *   <li>Construção de resposta estruturada</li>
 *   <li>Logging e auditoria</li>
 * </ul>
 * <p>
 * <b>Pipeline de processamento:</b>
 * <ol>
 *   <li><b>Geração:</b> cria transação via generator (automática) ou DTO (manual)</li>
 *   <li><b>Avaliação:</b> executa motor antifraude e calcula score de risco</li>
 *   <li><b>Persistência:</b> salva transação no banco de dados</li>
 *   <li><b>Alertas:</b> cria e persiste alertas de fraude se necessário</li>
 *   <li><b>Resposta:</b> monta DTO consolidado com todos os dados relevantes</li>
 * </ol>
 * <p>
 * <b>Características importantes:</b>
 * <ul>
 *   <li><b>Transacional:</b> todas as operações são atômicas (rollback em caso de erro)</li>
 *   <li><b>Auditável:</b> logs estruturados para rastreabilidade completa</li>
 *   <li><b>Seguro:</b> retorna DTOs sanitizados, nunca entidades diretas</li>
 *   <li><b>Flexível:</b> suporta fluxos automáticos e manuais</li>
 * </ul>
 * <p>
 * Este serviço deve ser o único ponto de entrada para processamento de transações,
 * garantindo consistência e aplicação uniforme de todas as políticas antifraude.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
public class TransactionPipelineService {


    private static final Logger log = LoggerFactory.getLogger(TransactionPipelineService.class);

    private final TransactionGenerator transactionGenerator;
    private final TransactionDecisionService decisionService;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertFactory fraudAlertFactory;

    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param transactionGenerator gerador responsável pela criação de transações
     * @param decisionService serviço de decisão antifraude
     * @param transactionRepository repositório para persistência de transações
     * @param fraudAlertRepository repositório para persistência de alertas de fraude
     * @param fraudAlertFactory factory para construção de alertas estruturados
     */
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
     * Processa uma transação através do pipeline completo de validação e persistência.
     * <p>
     * Este método orquestra todo o fluxo de processamento transacional:
     * <ol>
     *   <li><b>Geração da transação:</b>
     *       <ul>
     *         <li>Se manual: utiliza dados fornecidos no ManualTransactionDto</li>
     *         <li>Se automática: gera transação com dados aleatórios realistas</li>
     *       </ul>
     *   </li>
     *   <li><b>Avaliação antifraude:</b>
     *       <ul>
     *         <li>Executa todas as validações configuradas</li>
     *         <li>Calcula score de risco consolidado</li>
     *         <li>Identifica tipos de alerta disparados</li>
     *         <li>Define decisão (APPROVED, REVIEW, BLOCKED)</li>
     *       </ul>
     *   </li>
     *   <li><b>Persistência da transação:</b>
     *       <ul>
     *         <li>Salva transação com decisão e flags definidas</li>
     *         <li>Garante atomicidade via @Transactional</li>
     *       </ul>
     *   </li>
     *   <li><b>Criação de alertas:</b>
     *       <ul>
     *         <li>Se alertas foram disparados, cria FraudAlert</li>
     *         <li>Persiste alerta vinculado à transação</li>
     *         <li>Determina severidade para priorização</li>
     *       </ul>
     *   </li>
     *   <li><b>Construção da resposta:</b>
     *       <ul>
     *         <li>Monta DTO consolidado com todos os dados relevantes</li>
     *         <li>Inclui informações de cartão, dispositivo e localização</li>
     *         <li>Adiciona resultado da validação e severidade</li>
     *       </ul>
     *   </li>
     * </ol>
     * <p>
     * <b>Modos de operação:</b>
     * <table border="1">
     *   <tr>
     *     <th>Modo</th>
     *     <th>isManual</th>
     *     <th>successForce</th>
     *     <th>Comportamento</th>
     *   </tr>
     *   <tr>
     *     <td>Automático Normal</td>
     *     <td>false</td>
     *     <td>false</td>
     *     <td>Gera e processa transação com validação completa</td>
     *   </tr>
     *   <tr>
     *     <td>Automático Forçado</td>
     *     <td>false</td>
     *     <td>true</td>
     *     <td>Gera transação e força aprovação (teste)</td>
     *   </tr>
     *   <tr>
     *     <td>Manual Normal</td>
     *     <td>true</td>
     *     <td>false</td>
     *     <td>Usa DTO fornecido e processa normalmente</td>
     *   </tr>
     *   <tr>
     *     <td>Manual Forçado</td>
     *     <td>true</td>
     *     <td>true</td>
     *     <td>Usa DTO fornecido e força aprovação (teste)</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Características transacionais:</b>
     * <ul>
     *   <li><b>Atomicidade:</b> todas as operações são revertidas em caso de erro</li>
     *   <li><b>Consistência:</b> garante integridade entre transação e alertas</li>
     *   <li><b>Isolamento:</b> evita race conditions em operações concorrentes</li>
     *   <li><b>Durabilidade:</b> commit só após persistência completa</li>
     * </ul>
     * <p>
     * <b>Logging e auditoria:</b>
     * <ul>
     *   <li>Log INFO registrado após conclusão bem-sucedida</li>
     *   <li>Inclui ID da transação para rastreabilidade</li>
     *   <li>Logs automáticos de erro capturados pelo framework</li>
     * </ul>
     * <p>
     * <b>Segurança:</b>
     * <ul>
     *   <li>Retorna apenas DTO, nunca entidades JPA diretas</li>
     *   <li>Números de cartão podem ser mascarados no DTO</li>
     *   <li>Dados sensíveis protegidos conforme políticas PCI-DSS</li>
     * </ul>
     * <p>
     * <b>Performance:</b>
     * Em ambientes de alta carga, considere:
     * <ul>
     *   <li>Processamento assíncrono de alertas</li>
     *   <li>Batch processing para múltiplas transações</li>
     *   <li>Cache de padrões comportamentais</li>
     *   <li>Connection pooling adequado</li>
     * </ul>
     *
     * @param isManual se true, usa ManualTransactionDto; se false, gera automaticamente
     * @param successForce se true, força aprovação independente do score (para testes)
     * @param manualTransactionDto DTO com dados da transação manual (obrigatório se isManual = true)
     * @return TransactionResponseDto contendo todos os dados da transação processada, validação e alertas
     * @throws IllegalArgumentException se isManual = true mas manualTransactionDto é null
     * @throws RuntimeException se ocorrer erro durante processamento (rollback automático)
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


        log.info("Transação realizada com sucesso. || Id: {}.", transaction.getTransactionId());

        // =========================
        // 5️⃣ RETORNO DTO
        // =========================
        return new TransactionResponseDto(
                new CardDataResponseDto(
                        transaction.getCard().getCardId(),
                        transaction.getCard().getCardNumber(),
                        transaction.getCard().getCardHolderName(),
                        transaction.getCard().getCardBrand(),
                        transaction.getCard().getExpirationDate(),
                        transaction.getCard().getCreditLimit(),
                        transaction.getCard().getStatus()
                ),
                transaction.getMerchantCategory(),
                transaction.getAmount(),
                transaction.getReimbursement(),
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