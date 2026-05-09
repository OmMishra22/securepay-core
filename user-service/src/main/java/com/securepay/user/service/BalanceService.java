package com.securepay.user.service;

import com.securepay.user.exception.ResourceNotFoundException;
import com.securepay.user.exception.ValidationException;
import com.securepay.user.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.securepay.user.repository.UserRepository;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class BalanceService {
    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

    /**
     * SYSTEM account (userId = 0) represents the "outside world" — bank transfers,
     * card payments, UPI loads, etc. flowing INTO the platform.
     *
     * When money enters the system:  DEBIT SYSTEM (0)  → CREDIT User (N)
     * When money leaves the system:  DEBIT User (N)    → CREDIT SYSTEM (0)
     *
     * SYSTEM balance is always negative — its absolute value equals
     * the total money deposited across all users (platform liability).
     */
    private static final Long SYSTEM_ACCOUNT_ID = 0L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    private final String ledgerServiceUrl;
    private final String ledgerUser;
    private final String ledgerPassword;
    private final String internalSecret;

    public BalanceService(RestTemplate restTemplate, ObjectMapper objectMapper,
                          UserRepository userRepository, Environment environment) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.ledgerServiceUrl = environment.getProperty("LEDGER_URL", "http://ledger-service:8080");
        this.ledgerUser = environment.getProperty("LEDGER_USER", "ledger");
        this.ledgerPassword = environment.getProperty("LEDGER_PASSWORD", "ledger-password");
        this.internalSecret = environment.getProperty("INTERNAL_SECRET", "secret-key");
    }

    /**
     * Save a new user. If initial balance > 0, perform a double-entry deposit:
     *   DEBIT  SYSTEM account (money leaves the outside world)
     *   CREDIT user account   (money enters the user's wallet)
     */
    public ResponseEntity<User> saveUser(User user) {
        if (user.getBalance() == null) {
            user.setBalance(0.0);
        }
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());
        User savedUser = userRepository.save(user);

        // If user has an initial balance, perform double-entry deposit
        if (savedUser.getBalance() != null && savedUser.getBalance() > 0) {
            performDeposit(savedUser.getId(), savedUser.getBalance(), "Initial balance on account creation");
        }

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    /**
     * Deposit funds into a user's account using double-entry bookkeeping.
     *
     * Creates two ledger entries with the same transactionId:
     *   1. DEBIT  on SYSTEM account (userId=0) — money leaves the outside world
     *   2. CREDIT on user account             — money enters the user's wallet
     *
     * @param userId the user to credit
     * @param amount the amount to deposit (must be > 0)
     * @param description reason for the deposit
     */
    public void performDeposit(Long userId, Double amount, String description) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("userId must be greater than 0");
        }
        if (amount == null || amount <= 0) {
            throw new ValidationException("Deposit amount must be greater than 0");
        }

        // Generate a unique deposit transaction ID
        // Using negative timestamp to distinguish deposits from payment transactions
        Long depositTxnId = -System.nanoTime();

        try {
            // Step 1: DEBIT the SYSTEM account (money leaves the outside world)
            callLedger("/ledger/debit", Map.of(
                    "transactionId", depositTxnId,
                    "userId", SYSTEM_ACCOUNT_ID,
                    "amount", amount
            ));
            logger.info("Deposit DEBIT recorded: SYSTEM account debited {} for user {} (txnId={})",
                    amount, userId, depositTxnId);

            // Step 2: CREDIT the user account (money enters the user's wallet)
            callLedger("/ledger/credit", Map.of(
                    "transactionId", depositTxnId,
                    "userId", userId,
                    "amount", amount
            ));
            logger.info("Deposit CREDIT recorded: user {} credited {} (txnId={})",
                    userId, amount, depositTxnId);

            logger.info("Double-entry deposit completed: {} deposited to user {} [{}]",
                    amount, userId, description);

        } catch (Exception e) {
            logger.error("Failed to record deposit for user {}: {}", userId, e.getMessage(), e);
            throw new ValidationException("Failed to process deposit: " + e.getMessage());
        }
    }

    /**
     * Calculate the actual balance for a user by querying the ledger service.
     * Balance = SUM of all ledger entry amounts for the user.
     * (debits are negative, credits/reversals are positive)
     */
    public Double calculateBalance(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("UserId must be greater than 0");
        }

        try {
            String url = String.format("%s/ledger/history/%d", ledgerServiceUrl, userId);
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, entity, String.class
            ).getBody();

            if (response == null || response.isBlank()) {
                return 0.0;
            }

            double balance = parseAndCalculateBalance(response);
            logger.info("Calculated balance for user {}: {}", userId, balance);
            return balance;
        } catch (Exception e) {
            logger.error("Error calculating balance for user {}: {}", userId, e.getMessage(), e);
            throw new ValidationException("Failed to calculate balance: " + e.getMessage());
        }
    }

    /**
     * Get the SYSTEM account balance.
     * This value is always negative — its absolute value equals
     * the total money deposited across all users (platform liability).
     */
    public Double getSystemAccountBalance() {
        try {
            String url = String.format("%s/ledger/history/%d", ledgerServiceUrl, SYSTEM_ACCOUNT_ID);
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, entity, String.class
            ).getBody();

            if (response == null || response.isBlank()) {
                return 0.0;
            }
            return parseAndCalculateBalance(response);
        } catch (Exception e) {
            logger.error("Error fetching SYSTEM account balance: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Check if a user has sufficient balance for a transaction
     */
    public boolean hasSufficientBalance(Long userId, Double amount) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("UserId must be greater than 0");
        }
        if (amount == null || amount <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }
        Double currentBalance = calculateBalance(userId);
        return currentBalance >= amount;
    }

    /**
     * Get balance summary for a user
     */
    public BalanceSummary getBalanceSummary(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("UserId must be greater than 0");
        }
        Double currentBalance = calculateBalance(userId);
        BalanceSummary summary = new BalanceSummary();
        summary.setUserId(userId);
        summary.setCurrentBalance(currentBalance);
        summary.setTimestamp(System.currentTimeMillis());
        return summary;
    }

    // ─── Private helpers ──────────────────────────────────────

    /**
     * Call a ledger endpoint with the given payload
     */
    private void callLedger(String path, Map<String, Object> payload) {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        restTemplate.postForObject(ledgerServiceUrl + path, entity, String.class);
    }

    /**
     * Create authenticated headers for ledger service calls
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(ledgerUser, ledgerPassword);
        headers.set("X-Internal-Secret", internalSecret);
        return headers;
    }

    /**
     * Parse ledger response JSON and sum all transaction amounts.
     */
    private double parseAndCalculateBalance(String ledgerResponse) {
        if (ledgerResponse == null || ledgerResponse.isBlank()) {
            return 0.0;
        }
        try {
            List<Map<String, Object>> transactions = objectMapper.readValue(
                    ledgerResponse,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );
            double balance = 0.0;
            for (Map<String, Object> txn : transactions) {
                Object amountObj = txn.get("amount");
                if (amountObj != null) {
                    balance += Double.parseDouble(amountObj.toString());
                }
            }
            return balance;
        } catch (Exception e) {
            logger.error("Error parsing ledger response: {}", e.getMessage(), e);
            throw new ValidationException("Failed to parse ledger response");
        }
    }

    /**
     * DTO for balance summary
     */
    public static class BalanceSummary {
        private Long userId;
        private Double currentBalance;
        private Long timestamp;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Double getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(Double currentBalance) { this.currentBalance = currentBalance; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
