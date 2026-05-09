package com.securepay.ledger.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_txn_entry_user", columnList = "transactionId, entryType, userId", unique = true)
})
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long transactionId;
    private Long userId;
    private Double amount;
    private String entryType;
    private Instant createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(Long transactionId, Long userId, Double amount, String entryType) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.entryType = entryType;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
