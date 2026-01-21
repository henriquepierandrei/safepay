package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.entities.Transaction;
import tech.safepay.services.TransactionDecisionService;
import tech.safepay.services.TransactionPipelineService;
import tech.safepay.services.TransactionService;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/transaction")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionPipelineService transactionPipelineService;

    public TransactionController(TransactionService transactionService, TransactionPipelineService transactionPipelineService) {
        this.transactionService = transactionService;
        this.transactionPipelineService = transactionPipelineService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getTransactionById(@RequestParam(name = "transactionId") UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    /**
     * Simula uma nova transação passando por todo o pipeline antifraude.
     */
    @PostMapping("/process")
    public ResponseEntity<TransactionPipelineService.TransactionDecisionResponse> processTransaction() {
        // processa pipeline, já retorna DTO seguro
        var result = transactionPipelineService.process(false, null);
        return ResponseEntity.ok(result);
    }


    /**
     * Simula uma nova transação de forma manual passando por todo o pipeline antifraude.
     */
    @PostMapping("/manual")
    public ResponseEntity<TransactionPipelineService.TransactionDecisionResponse> processManualTransaction(@RequestBody ManualTransactionDto manualTransactionDto) {
        // processa pipeline, já retorna DTO seguro
        var result = transactionPipelineService.process(true, manualTransactionDto);
        return ResponseEntity.ok(result);
    }

}
