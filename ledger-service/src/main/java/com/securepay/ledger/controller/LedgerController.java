package com.securepay.ledger.controller;

import com.securepay.ledger.model.LedgerEntry;
import com.securepay.ledger.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/debit")
    public ResponseEntity<String> debit(@RequestBody Map<String, Object> payload,
                                        @RequestHeader("X-Internal-Secret") String secret) {
        ledgerService.validateSecret(secret);
        ledgerService.debit(payload);
        return ResponseEntity.ok("DEBIT_OK");
    }

    @PostMapping("/credit")
    public ResponseEntity<String> credit(@RequestBody Map<String, Object> payload,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        ledgerService.validateSecret(secret);
        ledgerService.credit(payload);
        return ResponseEntity.ok("CREDIT_OK");
    }

    @PostMapping("/reverse")
    public ResponseEntity<String> reverse(@RequestBody Map<String, Object> payload,
                                          @RequestHeader("X-Internal-Secret") String secret) {
        ledgerService.validateSecret(secret);
        ledgerService.reverse(payload);
        return ResponseEntity.ok("REVERSE_OK");
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<LedgerEntry>> history(@PathVariable Long userId,
                                                     @RequestHeader("X-Internal-Secret") String secret) {
        ledgerService.validateSecret(secret);
        return ResponseEntity.ok(ledgerService.getHistory(userId));
    }
}
