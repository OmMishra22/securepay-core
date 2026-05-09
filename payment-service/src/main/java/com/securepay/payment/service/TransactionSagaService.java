package com.securepay.payment.service;

import com.securepay.payment.model.PaymentRequest;
import com.securepay.payment.model.PaymentResponse;
import com.securepay.payment.model.Transaction;
import com.securepay.payment.model.TransactionState;
import com.securepay.payment.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TransactionSagaService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionSagaService.class);

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ledgerUrl;
    private final String notificationUrl;
    private final String internalSecret;
    private final String ledgerUser;
    private final String ledgerPassword;
    private final String notificationUser;
    private final String notificationPassword;

    public TransactionSagaService(TransactionRepository transactionRepository,
                                  RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  org.springframework.core.env.Environment environment) {
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        // Updated URLs to use internal network endpoints
        this.ledgerUrl = environment.getProperty("LEDGER_URL", "http://ledger-service:8080");
        this.notificationUrl = environment.getProperty("NOTIFICATION_URL", "http://notification-service:8080");
        this.internalSecret = environment.getProperty("INTERNAL_SECRET", "secret-key");
        this.ledgerUser = environment.getProperty("LEDGER_USER", "ledger");
        this.ledgerPassword = environment.getProperty("LEDGER_PASSWORD", "ledger-password");
        this.notificationUser = environment.getProperty("NOTIFICATION_USER", "notification");
        this.notificationPassword = environment.getProperty("NOTIFICATION_PASSWORD", "notification-password");
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        String correlationId = request.getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "corr-" + Instant.now().toEpochMilli();
            request.setCorrelationId(correlationId);
        }

        // Idempotency check: if a transaction with this correlationId already exists in a final state, return it
        Transaction existing = transactionRepository.findByCorrelationId(correlationId).orElse(null);
        if (existing != null && isFinalState(existing.getState())) {
            logger.info("Idempotent replay for correlationId={}, returning existing txnId={} status={}",
                    correlationId, existing.getId(), existing.getState());
            PaymentResponse response = new PaymentResponse(existing.getId(), existing.getState().name(), 
                                                          "Idempotent replay returned existing status");
            response.setCorrelationId(correlationId);
            return response;
        }

        Transaction transaction = existing == null ?
                transactionRepository.save(new Transaction(request.getFromUserId(), request.getToUserId(), request.getAmount(), correlationId)) :
                existing;

        logger.info("Processing payment: txnId={} from={} to={} amount={} correlationId={}",
                transaction.getId(), transaction.getFromUserId(), transaction.getToUserId(), 
                transaction.getAmount(), correlationId);

        try {
            if (transaction.getState() == TransactionState.INITIATED) {
                executeDebit(transaction);
            }
            if (transaction.getState() == TransactionState.DEBITED) {
                executeCredit(transaction);
            }
            if (transaction.getState() == TransactionState.CREDITED) {
                notifySuccess(transaction);
                // Get updated balances after successful transaction
                Double fromUserBalance = getBalance(transaction.getFromUserId());
                Double toUserBalance = getBalance(transaction.getToUserId());
                
                logger.info("Payment completed: txnId={} fromBalance={} toBalance={}",
                        transaction.getId(), fromUserBalance, toUserBalance);

                PaymentResponse response = new PaymentResponse(transaction.getId(), transaction.getState().name(), 
                                                              "Payment completed successfully", 
                                                              fromUserBalance, toUserBalance);
                response.setCorrelationId(correlationId);
                return response;
            }
        } catch (Exception ex) {
            logger.error("Payment failed: txnId={} reason={}", transaction.getId(), ex.getMessage(), ex);
            return handleFailure(transaction, ex.getMessage());
        }

        PaymentResponse response = new PaymentResponse(transaction.getId(), transaction.getState().name(), "Transaction in progress");
        response.setCorrelationId(correlationId);
        return response;
    }

    /**
     * Get payment history for a specific user using a targeted database query
     */
    public String getHistory(Long userId) {
        logger.info("Fetching payment history for userId={}", userId);
        return transactionRepository.findByFromUserIdOrToUserId(userId, userId).stream()
                .map(it -> String.format("%d %s %s -> %s $%.2f", it.getId(), it.getState(), it.getFromUserId(), it.getToUserId(), it.getAmount()))
                .reduce("", (acc, item) -> acc + item + "\n");
    }

    private void executeDebit(Transaction transaction) {
        logger.info("Saga step DEBIT: txnId={} userId={} amount={}", 
                transaction.getId(), transaction.getFromUserId(), transaction.getAmount());

        HttpHeaders headers = createLedgerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
                "transactionId", transaction.getId(),
                "userId", transaction.getFromUserId(),
                "amount", transaction.getAmount()
        );

        restTemplate.postForObject(URI.create(ledgerUrl + "/ledger/debit"), new HttpEntity<>(payload, headers), String.class);
        transaction.setState(TransactionState.DEBITED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);

        logger.info("Saga step DEBIT completed: txnId={}", transaction.getId());
    }

    private void executeCredit(Transaction transaction) {
        logger.info("Saga step CREDIT: txnId={} userId={} amount={}", 
                transaction.getId(), transaction.getToUserId(), transaction.getAmount());

        HttpHeaders headers = createLedgerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
                "transactionId", transaction.getId(),
                "userId", transaction.getToUserId(),
                "amount", transaction.getAmount()
        );

        try {
            restTemplate.postForObject(URI.create(ledgerUrl + "/ledger/credit"), new HttpEntity<>(payload, headers), String.class);
            transaction.setState(TransactionState.CREDITED);
            transaction.setUpdatedAt(Instant.now());
            transactionRepository.save(transaction);
            logger.info("Saga step CREDIT completed: txnId={}", transaction.getId());
        } catch (Exception ex) {
            logger.error("Saga step CREDIT failed, initiating reversal: txnId={} reason={}", 
                    transaction.getId(), ex.getMessage());
            reverseDebit(transaction);
            throw new IllegalStateException("Credit failed, reversal executed");
        }
    }

    private void reverseDebit(Transaction transaction) {
        logger.info("Saga COMPENSATING step: reversing debit for txnId={} userId={} amount={}", 
                transaction.getId(), transaction.getFromUserId(), transaction.getAmount());

        HttpHeaders headers = createLedgerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
                "transactionId", transaction.getId(),
                "userId", transaction.getFromUserId(),
                "amount", transaction.getAmount()
        );
        restTemplate.postForObject(URI.create(ledgerUrl + "/ledger/reverse"), new HttpEntity<>(payload, headers), String.class);
        transaction.setState(TransactionState.REVERSED);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);
        notifyFailure(transaction, "Debit reversal completed after credit failure");

        logger.info("Saga COMPENSATING step completed: txnId={} state=REVERSED", transaction.getId());
    }

    private void notifySuccess(Transaction transaction) {
        sendNotification(transaction, "Payment completed for transaction " + transaction.getId());
    }

    private void notifyFailure(Transaction transaction, String reason) {
        sendNotification(transaction, "Payment failed: " + reason);
    }

    private void sendNotification(Transaction transaction, String message) {
        try {
            HttpHeaders headers = createNotificationHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of(
                    "userId", transaction.getFromUserId(),
                    "message", message
            );
            restTemplate.postForObject(URI.create(notificationUrl + "/notifications"), new HttpEntity<>(payload, headers), String.class);
            logger.info("Notification sent: txnId={} message={}", transaction.getId(), message);
        } catch (Exception ex) {
            // Notification failure should not break the payment flow
            logger.warn("Failed to send notification for txnId={}: {}", transaction.getId(), ex.getMessage());
        }
    }

    private PaymentResponse handleFailure(Transaction transaction, String reason) {
        transaction.setState(TransactionState.FAILED);
        transaction.setFailureReason(reason);
        transaction.setUpdatedAt(Instant.now());
        transactionRepository.save(transaction);
        notifyFailure(transaction, reason);
        return new PaymentResponse(transaction.getId(), transaction.getState().name(), reason);
    }

    private HttpHeaders createLedgerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.setBasicAuth(ledgerUser, ledgerPassword);
        return headers;
    }

    private HttpHeaders createNotificationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.setBasicAuth(notificationUser, notificationPassword);
        return headers;
    }

    private boolean isFinalState(TransactionState state) {
        return state == TransactionState.CREDITED || state == TransactionState.FAILED || state == TransactionState.REVERSED;
    }

    /**
     * Get the balance for a user by querying the ledger service history
     * and summing all ledger entry amounts (debits are negative, credits are positive).
     */
    private Double getBalance(Long userId) {
        try {
            HttpHeaders headers = createLedgerHeaders();
            String url = ledgerUrl + "/ledger/history/" + userId;
            String response = restTemplate.exchange(URI.create(url), HttpMethod.GET,
                    new HttpEntity<>(headers), String.class).getBody();
            
            if (response == null || response.isBlank()) {
                return 0.0;
            }

            // Parse the JSON array of ledger entries and sum all amounts
            List<Map<String, Object>> entries = objectMapper.readValue(
                    response,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );

            double balance = 0.0;
            for (Map<String, Object> entry : entries) {
                Object amountObj = entry.get("amount");
                if (amountObj != null) {
                    balance += Double.parseDouble(amountObj.toString());
                }
            }

            logger.info("Retrieved balance for userId={}: {}", userId, balance);
            return balance;
        } catch (Exception ex) {
            logger.warn("Failed to retrieve balance for userId={}: {}", userId, ex.getMessage());
            return 0.0;
        }
    }
}
