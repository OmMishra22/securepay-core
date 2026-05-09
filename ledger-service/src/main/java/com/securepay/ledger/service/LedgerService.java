package com.securepay.ledger.service;

import com.securepay.ledger.exception.UnauthorizedException;
import com.securepay.ledger.exception.ValidationException;
import com.securepay.ledger.model.LedgerEntry;
import com.securepay.ledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class LedgerService {
    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    /**
     * SYSTEM account (userId=0) is a special teller account used for double-entry bookkeeping.
     * It represents money flowing in/out of the platform from external sources (bank, UPI, card).
     * Its balance is always negative — the absolute value = total platform liability.
     */
    private static final Long SYSTEM_ACCOUNT_ID = 0L;

    private final LedgerRepository ledgerRepository;
    private final String internalSecret;

    public LedgerService(LedgerRepository ledgerRepository, org.springframework.core.env.Environment environment) {
        this.ledgerRepository = ledgerRepository;
        this.internalSecret = environment.getProperty("INTERNAL_SECRET", "secret-key");
    }

    public void validateSecret(String secret) {
        if (secret == null || !internalSecret.equals(secret)) {
            logger.warn("Invalid internal secret received");
            throw new UnauthorizedException("Invalid internal secret");
        }
    }

    @Transactional
    public void debit(Map<String, Object> payload) {
        if (payload == null) {
            throw new ValidationException("Payload cannot be null");
        }

        Long transactionId = extractLong(payload, "transactionId");
        Long userId = extractLong(payload, "userId");
        Double amount = extractDouble(payload, "amount");

        // userId=0 (SYSTEM account) is allowed for deposits
        if (userId < 0) throw new ValidationException("userId cannot be negative");
        if (amount <= 0) throw new ValidationException("amount must be greater than 0");

        if (ledgerRepository.findByTransactionIdAndEntryTypeAndUserId(transactionId, "DEBIT", userId).isPresent()) {
            logger.info("Idempotent skip: DEBIT already exists for txnId={} userId={}", transactionId, userId);
            return;
        }

        LedgerEntry entry = new LedgerEntry(transactionId, userId, -Math.abs(amount), "DEBIT");
        ledgerRepository.save(entry);
        logger.info("DEBIT recorded: txnId={} userId={} amount=-{}", transactionId, userId, Math.abs(amount));
    }

    @Transactional
    public void credit(Map<String, Object> payload) {
        if (payload == null) {
            throw new ValidationException("Payload cannot be null");
        }

        Long transactionId = extractLong(payload, "transactionId");
        Long userId = extractLong(payload, "userId");
        Double amount = extractDouble(payload, "amount");

        // userId=0 (SYSTEM account) is allowed for withdrawals
        if (userId < 0) throw new ValidationException("userId cannot be negative");
        if (amount <= 0) throw new ValidationException("amount must be greater than 0");

        if (ledgerRepository.findByTransactionIdAndEntryTypeAndUserId(transactionId, "CREDIT", userId).isPresent()) {
            logger.info("Idempotent skip: CREDIT already exists for txnId={} userId={}", transactionId, userId);
            return;
        }

        LedgerEntry entry = new LedgerEntry(transactionId, userId, Math.abs(amount), "CREDIT");
        ledgerRepository.save(entry);
        logger.info("CREDIT recorded: txnId={} userId={} amount=+{}", transactionId, userId, Math.abs(amount));
    }

    @Transactional
    public void reverse(Map<String, Object> payload) {
        if (payload == null) {
            throw new ValidationException("Payload cannot be null");
        }

        Long transactionId = extractLong(payload, "transactionId");
        Long userId = extractLong(payload, "userId");
        Double amount = extractDouble(payload, "amount");

        if (userId < 0) throw new ValidationException("userId cannot be negative");
        if (amount <= 0) throw new ValidationException("amount must be greater than 0");

        if (ledgerRepository.findByTransactionIdAndEntryTypeAndUserId(transactionId, "REVERSE", userId).isPresent()) {
            logger.info("Idempotent skip: REVERSE already exists for txnId={} userId={}", transactionId, userId);
            return;
        }

        LedgerEntry entry = new LedgerEntry(transactionId, userId, Math.abs(amount), "REVERSE");
        ledgerRepository.save(entry);
        logger.info("REVERSE recorded: txnId={} userId={} amount=+{}", transactionId, userId, Math.abs(amount));
    }

    public List<LedgerEntry> getHistory(Long userId) {
        if (userId == null || userId < 0) {
            throw new ValidationException("userId cannot be negative");
        }
        logger.info("Fetching ledger history for userId={}", userId);
        return ledgerRepository.findByUserId(userId);
    }

    /**
     * Safely extract a Long value from the payload map.
     */
    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new ValidationException(key + " is required");
        }
        try {
            return ((Number) value).longValue();
        } catch (ClassCastException e) {
            throw new ValidationException(key + " must be a valid number");
        }
    }

    /**
     * Safely extract a Double value from the payload map.
     */
    private Double extractDouble(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new ValidationException(key + " is required");
        }
        try {
            return ((Number) value).doubleValue();
        } catch (ClassCastException e) {
            throw new ValidationException(key + " must be a valid number");
        }
    }
}
