package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.safepay.services.TransactionService;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/transaction")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getTransactionById(@RequestParam(name = "transactionId") UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
