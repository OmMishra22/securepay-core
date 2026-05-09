package com.securepay.user.model;

import jakarta.validation.constraints.*;

public class UserPaymentRequest {
    @NotNull(message = "toUserId is required")
    @Positive(message = "toUserId must be greater than 0")
    private Long toUserId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0.01")
    @DecimalMax(value = "999999.99", message = "amount cannot exceed 999999.99")
    private Double amount;

    @Size(min = 5, max = 50, message = "correlationId must be between 5 and 50 characters")
    private String correlationId;

    // fromUserId is set by the controller from the path parameter
    private Long fromUserId;

    public UserPaymentRequest() {}

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
