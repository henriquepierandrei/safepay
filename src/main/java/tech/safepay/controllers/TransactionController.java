package tech.safepay.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.services.TransactionService;
import tech.safepay.services.TransactionPipelineService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller responsável pelo processamento e consulta de transações no sistema SafePay.
 * <p>
 * Gerencia o fluxo de transações através do pipeline antifraude,
 * permitindo processamento automático e manual, além de consultas
 * por identificador.
 * </p>
 *
 * Base path: {@code /api/v1/transaction}
 *
 * @author SafePay Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    /**
     * Serviço responsável pela consulta e recuperação de transações.
     */
    private final TransactionService transactionService;

    /**
     * Serviço responsável pela orquestração do pipeline antifraude.
     */
    private final TransactionPipelineService transactionPipelineService;

    /**
     * Construtor do {@link TransactionController}.
     *
     * @param transactionService serviço de transações
     * @param transactionPipelineService serviço do pipeline antifraude
     */
    public TransactionController(
            TransactionService transactionService,
            TransactionPipelineService transactionPipelineService
    ) {
        this.transactionService = transactionService;
        this.transactionPipelineService = transactionPipelineService;
    }

    /**
     * Retorna os dados completos de uma transação a partir do seu identificador.
     *
     * Exemplo:
     * {@code GET /api/v1/transaction/get?transactionId=UUID}
     *
     * @param transactionId identificador único da transação
     * @return resposta contendo os dados completos da transação
     */
    @GetMapping("/get")
    public ResponseEntity<?> getTransactionById(
            @RequestParam(name = "transactionId") UUID transactionId
    ) {
        return ResponseEntity.ok(
                transactionService.getTransactionById(transactionId)
        );
    }

    @GetMapping("/get/filters")
    public Page<TransactionResponseDto> getTransactions(
            @RequestParam UUID cardId,
            @RequestParam UUID deviceId,
            @RequestParam(required = false) Boolean isReimbursement,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort direction = sort.equalsIgnoreCase("asc")
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, direction);

        return transactionService.findTransactions(
                cardId,
                deviceId,
                isReimbursement,
                startDate,
                endDate,
                pageable
        );
    }



    /**
     * Processa uma nova transação simulada através do pipeline antifraude.
     * <p>
     * A transação é gerada automaticamente com dados simulados,
     * sendo útil para testes e validação do motor antifraude.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/transaction/process?successForce=false}
     *
     * @param successForce quando {@code true}, força a aprovação da transação,
     *                     ignorando as validações antifraude
     * @return resposta contendo o resultado do processamento,
     *         incluindo score de fraude e decisão final
     */
    @PostMapping("/process")
    public ResponseEntity<?> processTransaction(
            @RequestParam(name = "successForce") boolean successForce
    ) {
        return ResponseEntity.ok(
                transactionPipelineService.process(false, successForce, null)
        );
    }

    /**
     * Processa uma transação manual através do pipeline antifraude.
     * <p>
     * Permite informar dados customizados como cartão, dispositivo,
     * valor e identificador do comerciante.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/transaction/manual?successForce=false}
     *
     * Body:
     * <pre>
     * {
     *   "cardId": "UUID",
     *   "deviceId": "UUID",
     *   "amount": 150.00,
     *   "merchantCategory": "MERCHANT_CATEGORY",
     *   "ipAddress": "ipv6",
     *   "latitude": "0",
     *   "longitude": "0"
     * }
     * </pre>
     *
     * @param successForce quando {@code true}, força a aprovação da transação,
     *                     ignorando as validações antifraude
     * @param manualTransactionDto objeto contendo os dados da transação manual
     * @return resposta contendo o resultado do processamento,
     *         incluindo score de fraude e decisão final
     */
    @PostMapping("/manual")
    public ResponseEntity<?> processManualTransaction(
            @RequestParam(name = "successForce") boolean successForce,
            @RequestBody ManualTransactionDto manualTransactionDto
    ) {
        return ResponseEntity.ok(
                transactionPipelineService.process(
                        true,
                        successForce,
                        manualTransactionDto
                )
        );
    }
}
