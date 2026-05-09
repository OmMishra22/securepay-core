package com.securepay.user.controller;

import com.securepay.user.exception.ResourceNotFoundException;
import com.securepay.user.exception.ValidationException;
import com.securepay.user.model.DepositRequest;
import com.securepay.user.model.User;
import com.securepay.user.model.UserPaymentRequest;
import com.securepay.user.model.UserPaymentResponse;
import com.securepay.user.repository.UserRepository;
import com.securepay.user.service.BalanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final BalanceService balanceService;

    public UserController(UserRepository userRepository, RestTemplate restTemplate, BalanceService balanceService) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.balanceService = balanceService;
    }

    /**
     * Create a new user with validation and email uniqueness.
     * If initial balance > 0, a double-entry deposit is recorded in the ledger:
     *   DEBIT  SYSTEM account (userId=0)
     *   CREDIT new user account
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        // Check for duplicate email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ValidationException("User with email " + user.getEmail() + " already exists");
        }

        return balanceService.saveUser(user);
    }

    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a specific user
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return ResponseEntity.ok(user);
    }

    /**
     * Get user's current balance (calculated from ledger entries)
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getUserBalance(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        Double balance = balanceService.calculateBalance(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("balance", balance);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Deposit funds into a user's account.
     * Uses double-entry bookkeeping:
     *   DEBIT  SYSTEM account (userId=0) — money enters the platform from external source
     *   CREDIT user account              — money is added to user's wallet
     *
     * Example request:
     *   POST /users/1/deposit
     *   { "amount": 5000.00, "description": "Bank transfer deposit" }
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Map<String, Object>> depositFunds(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {

        // Verify user exists
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            description = "Manual deposit";
        }

        // Perform double-entry deposit: DEBIT SYSTEM → CREDIT User
        balanceService.performDeposit(id, request.getAmount(), description);

        // Fetch updated balance
        Double newBalance = balanceService.calculateBalance(id);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("name", user.getName());
        response.put("depositAmount", request.getAmount());
        response.put("description", description);
        response.put("newBalance", newBalance);
        response.put("message", "Deposit successful");
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get the SYSTEM account balance (platform liability).
     * The absolute value of this number = total money deposited across all users.
     */
    @GetMapping("/system/balance")
    public ResponseEntity<Map<String, Object>> getSystemBalance() {
        Double systemBalance = balanceService.getSystemAccountBalance();

        Map<String, Object> response = new HashMap<>();
        response.put("systemAccountId", 0);
        response.put("systemBalance", systemBalance);
        response.put("totalPlatformDeposits", Math.abs(systemBalance));
        response.put("description", "SYSTEM account balance represents total platform liability. "
                + "Its absolute value equals the total money deposited across all users.");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Initiate a payment from one user to another
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<UserPaymentResponse> initiatePayment(
            @PathVariable Long id,
            @Valid @RequestBody UserPaymentRequest request) {
        
        // Verify user exists
        User fromUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Validate that fromUserId matches the path parameter
        if (!id.equals(request.getFromUserId())) {
            request.setFromUserId(id);
        }

        // Validate that recipient user exists
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found with ID: " + request.getToUserId()));

        // Check sufficient balance
        Double currentBalance = balanceService.calculateBalance(id);
        if (currentBalance < request.getAmount()) {
            throw new ValidationException("Insufficient balance. Current balance: " + currentBalance + 
                                        ", Required: " + request.getAmount());
        }

        // Call payment service with proper headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("payment", "payment-password");
        HttpEntity<UserPaymentRequest> entity = new HttpEntity<>(request, headers);
        
        // Use internal network URL
        UserPaymentResponse response = restTemplate.postForObject(
                URI.create("http://payment-service:8080/payments"), entity, UserPaymentResponse.class);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment history for a user
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<String> getPaymentHistory(@PathVariable Long id) {
        // Verify user exists
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Use internal network URL
        String url = String.format("http://payment-service:8080/payments/history/%d", id);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("payment", "payment-password");
        String history = restTemplate.exchange(URI.create(url), org.springframework.http.HttpMethod.GET, 
                                              new HttpEntity<>(headers), String.class).getBody();
        return ResponseEntity.ok(history);
    }
}
