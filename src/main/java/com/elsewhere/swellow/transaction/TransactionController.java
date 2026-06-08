package com.elsewhere.swellow.transaction;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@RequestBody Map<String, Object> payload) {
        Object receiverObj = payload.get("receiverWalletId");
        Object amountObj = payload.get("amount");

        if (receiverObj == null || amountObj == null) {
            throw new IllegalArgumentException("receiverWalletId and amount are required");
        }

        Long receiverWalletId = Long.valueOf(receiverObj.toString());
        BigDecimal amount = new BigDecimal(amountObj.toString());

        return ResponseEntity.ok(transactionService.transfer(receiverWalletId, amount));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionService.getTransactionsForCurrentUser());
    }
}
