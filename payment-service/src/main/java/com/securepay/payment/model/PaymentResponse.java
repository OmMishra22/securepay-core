package com.securepay.payment.model;

public class PaymentResponse {
    private Long transactionId;
    private String status;
    private String message;
    private Double fromUserBalance;      // Balance of sender after transaction
    private Double toUserBalance;        // Balance of receiver after transaction
    private String correlationId;        // For idempotency tracking

    public PaymentResponse() {
    }

    public PaymentResponse(Long transactionId, String status, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
    }

    public PaymentResponse(Long transactionId, String status, String message, 
                         Double fromUserBalance, Double toUserBalance) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.fromUserBalance = fromUserBalance;
        this.toUserBalance = toUserBalance;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getFromUserBalance() {
        return fromUserBalance;
    }

    public void setFromUserBalance(Double fromUserBalance) {
        this.fromUserBalance = fromUserBalance;
    }

    public Double getToUserBalance() {
        return toUserBalance;
    }

    public void setToUserBalance(Double toUserBalance) {
        this.toUserBalance = toUserBalance;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
