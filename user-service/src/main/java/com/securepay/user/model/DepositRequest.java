package com.securepay.user.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for depositing funds into a user account.
 * Deposits are processed as double-entry bookkeeping:
 *   DEBIT  SYSTEM account (userId=0) — money leaves the outside world
 *   CREDIT user account              — money enters the user's wallet
 */
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Deposit amount cannot exceed 999999.99")
    private Double amount;

    private String description;

    public DepositRequest() {}

    public DepositRequest(Double amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
