package com.securepay.payment.controller;

import com.securepay.payment.model.PaymentRequest;
import com.securepay.payment.model.PaymentResponse;
import com.securepay.payment.service.TransactionSagaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final TransactionSagaService sagaService;

    public PaymentController(TransactionSagaService sagaService) {
        this.sagaService = sagaService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        if (request.getCorrelationId() == null || request.getCorrelationId().isBlank()) {
            request.setCorrelationId("corr-" + System.currentTimeMillis());
        }
        PaymentResponse response = sagaService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<String> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(sagaService.getHistory(userId));
    }
}
