# SecurePayCore - API Reference Guide

## BASE URLS
- **Public API Gateway**: `http://localhost:8080`
- **Internal Services**: Not directly accessible (hidden on internal network)

---

## USER MANAGEMENT ENDPOINTS

### Create User (with validation)
```bash
POST /users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "555-123-4567"
}

# Success Response (201 Created)
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "555-123-4567",
  "balance": 0.0,
  "createdAt": 1713607800000,
  "updatedAt": 1713607800000
}

# Error Response (400 Bad Request) - Duplicate Email
{
  "errorCode": "VALIDATION_ERROR",
  "message": "User with email john@example.com already exists",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

# Error Response (400 Bad Request) - Invalid Email
{
  "errorCode": "VALIDATION_ERROR",
  "message": "email: Email must be valid",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users",
  "traceId": "x1y2z3a4-b5c6-7890-defg-hi1234567890"
}
```

### Get User Details
```bash
GET /users/{userId}

# Success Response (200 OK)
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "555-123-4567",
  "balance": 1000.50,
  "createdAt": 1713607800000,
  "updatedAt": 1713607800000
}

# Error Response (404 Not Found)
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with ID: 999",
  "statusCode": 404,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/999",
  "traceId": "error-trace-id"
}
```

### Get All Users
```bash
GET /users

# Success Response (200 OK)
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "555-123-4567",
    "balance": 1000.50,
    ...
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "phone": "555-987-6543",
    "balance": 2500.00,
    ...
  }
]
```

### Get User Balance
```bash
GET /users/{userId}/balance

# Success Response (200 OK)
{
  "userId": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "balance": 1000.50,
  "timestamp": 1713607800000
}

# Error Response (404 Not Found)
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with ID: 999",
  "statusCode": 404,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/999/balance",
  "traceId": "error-trace-id"
}
```

---

## BANK ACCOUNT MANAGEMENT ENDPOINTS

### Link Bank Account
```bash
POST /users/{userId}/bank-accounts
Content-Type: application/json

{
  "bankName": "Chase Bank",
  "accountNumber": "123456789012",
  "accountHolderName": "John Doe",
  "routingNumber": "021000021",
  "accountType": "CHECKING"
}

# Success Response (201 Created)
{
  "id": 1,
  "user": { "id": 1 },
  "bankName": "Chase Bank",
  "accountNumber": "123456789012",
  "accountHolderName": "John Doe",
  "routingNumber": "021000021",
  "accountType": "CHECKING",
  "status": "PENDING_VERIFICATION",
  "isPrimary": true,
  "createdAt": 1713607800000,
  "updatedAt": 1713607800000
}

# Error Response (400 Bad Request) - Invalid Routing Number
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid routing number format. Must be 9 digits.",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/1/bank-accounts",
  "traceId": "error-trace-id"
}
```

### Get All Bank Accounts for User
```bash
GET /users/{userId}/bank-accounts

# Success Response (200 OK)
[
  {
    "id": 1,
    "bankName": "Chase Bank",
    "accountNumber": "123456789012",
    "accountHolderName": "John Doe",
    "routingNumber": "021000021",
    "accountType": "CHECKING",
    "status": "PENDING_VERIFICATION",
    "isPrimary": true,
    "createdAt": 1713607800000,
    "updatedAt": 1713607800000
  }
]
```

### Get Specific Bank Account
```bash
GET /users/{userId}/bank-accounts/{accountId}

# Success Response (200 OK)
{
  "id": 1,
  "bankName": "Chase Bank",
  "accountNumber": "123456789012",
  "accountHolderName": "John Doe",
  "routingNumber": "021000021",
  "accountType": "CHECKING",
  "status": "PENDING_VERIFICATION",
  "isPrimary": true,
  "createdAt": 1713607800000,
  "updatedAt": 1713607800000
}
```

### Update Bank Account
```bash
PUT /users/{userId}/bank-accounts/{accountId}
Content-Type: application/json

{
  "accountHolderName": "John Michael Doe",
  "accountType": "SAVINGS"
}

# Success Response (200 OK) - Updated account
```

### Verify Bank Account
```bash
POST /users/{userId}/bank-accounts/{accountId}/verify
Content-Type: application/json

{
  "verificationCode": "micro-deposit-123"
}

# Success Response (200 OK)
{
  "id": 1,
  "bankName": "Chase Bank",
  "status": "ACTIVE",  # Changed from PENDING_VERIFICATION
  ...
}
```

### Set Primary Bank Account
```bash
POST /users/{userId}/bank-accounts/{accountId}/set-primary

# Success Response (200 OK)
{
  "id": 1,
  "bankName": "Chase Bank",
  "status": "ACTIVE",
  "isPrimary": true,  # Now true
  ...
}

# Error Response (400 Bad Request) - Account not verified
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Only verified (ACTIVE) bank accounts can be set as primary",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/1/bank-accounts/1/set-primary",
  "traceId": "error-trace-id"
}
```

### Get Primary Bank Account
```bash
GET /users/{userId}/bank-accounts/primary

# Success Response (200 OK)
{
  "id": 1,
  "bankName": "Chase Bank",
  "status": "ACTIVE",
  "isPrimary": true,
  ...
}

# Error Response (404 Not Found)
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "No primary bank account found for user: 1",
  "statusCode": 404,
  ...
}
```

### Delete Bank Account
```bash
DELETE /users/{userId}/bank-accounts/{accountId}

# Success Response (204 No Content)

# Error Response (400 Bad Request) - Only account
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Cannot delete the only bank account linked to this user",
  "statusCode": 400,
  ...
}
```

---

## PAYMENT ENDPOINTS

### Initiate Payment
```bash
POST /users/{userId}/payments
Content-Type: application/json

{
  "toUserId": 2,
  "amount": 100.50,
  "correlationId": "corr-unique-id-12345"  # Optional, auto-generated if missing
}

# Success Response (200 OK)
{
  "transactionId": 501,
  "status": "CREDITED",
  "message": "Payment completed successfully",
  "fromUserBalance": 899.50,
  "toUserBalance": 2100.50,
  "correlationId": "corr-unique-id-12345"
}

# Error Response (400 Bad Request) - Insufficient Balance
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Insufficient balance. Current balance: 50.00, Required: 100.50",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/1/payments",
  "traceId": "error-trace-id"
}

# Error Response (400 Bad Request) - Invalid Amount
{
  "errorCode": "VALIDATION_ERROR",
  "message": "fromUserId: fromUserId is required, toUserId: toUserId is required, amount: amount must be greater than 0",
  "statusCode": 400,
  "timestamp": "2026-04-19T10:30:00.000Z",
  "path": "/users/1/payments",
  "traceId": "error-trace-id"
}

# Error Response (404 Not Found) - User/Recipient not found
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with ID: 999",
  "statusCode": 404,
  ...
}
```

### Get Payment History
```bash
GET /users/{userId}/history

# Success Response (200 OK)
501 CREDITED 1 -> 2 $100.50
502 CREDITED 1 -> 3 $250.00
...
```

---

## ERROR CODES REFERENCE

| Error Code | HTTP Status | Meaning |
|-----------|-------------|---------|
| VALIDATION_ERROR | 400 | Input validation failed |
| UNAUTHORIZED | 401 | Authentication/Authorization failed |
| RESOURCE_NOT_FOUND | 404 | Resource does not exist |
| INTERNAL_SERVER_ERROR | 500 | Unexpected server error |
| ENDPOINT_NOT_FOUND | 404 | API endpoint does not exist |

---

## VALIDATION CONSTRAINTS

### User Fields
- **Name**: Required, non-blank
- **Email**: Required, valid email format, UNIQUE across system
- **Phone**: Required, valid phone number format (digits, +, -, spaces, parentheses)
- **Balance**: Non-negative (auto-set to 0.0 on creation)

### Payment Fields
- **toUserId**: Required, must be > 0
- **amount**: Required, must be between 0.01 and 999,999.99
- **correlationId**: Optional, if provided must be 5-50 characters

### Bank Account Fields
- **bankName**: Required, non-blank
- **accountNumber**: Required, 8-17 digits
- **accountHolderName**: Required, non-blank
- **routingNumber**: Required, exactly 9 digits
- **accountType**: Required, non-blank (CHECKING, SAVINGS, etc.)

---

## IDEMPOTENCY

All payment requests are idempotent using `correlationId`:
- Same `correlationId` with final state transaction returns cached result
- Prevents double-charging on network retries
- Unique `correlationId` auto-generated if not provided

Example:
```bash
# First request
POST /users/1/payments
{
  "toUserId": 2,
  "amount": 100.00,
  "correlationId": "payment-001"
}
# Response: CREDITED, balance updated

# Second request (same correlationId)
POST /users/1/payments
{
  "toUserId": 2,
  "amount": 100.00,
  "correlationId": "payment-001"
}
# Response: CREDITED, returns cached result (no duplicate charge)
```

---

## COMMON ERROR SCENARIOS

### Duplicate Email
```bash
POST /users
{
  "name": "Jane",
  "email": "john@example.com",  # Already exists
  "phone": "5551234567"
}
# 400 Bad Request - User with email john@example.com already exists
```

### Insufficient Balance
```bash
POST /users/1/payments
{
  "toUserId": 2,
  "amount": 10000.00  # Balance is only 1000.00
}
# 400 Bad Request - Insufficient balance
```

### Invalid Routing Number
```bash
POST /users/1/bank-accounts
{
  "bankName": "Chase",
  "accountNumber": "123456789012",
  "accountHolderName": "John Doe",
  "routingNumber": "12345",  # Only 5 digits, needs 9
  "accountType": "CHECKING"
}
# 400 Bad Request - Invalid routing number format
```

### User Not Found
```bash
GET /users/999
# 404 Not Found - User not found with ID: 999
```

---

## NEXT STEPS FOR INTEGRATION

1. **Bank API Integration**: Implement Plaid/Stripe integration for actual bank connections
2. **Add Funds**: Create endpoint to add funds (via card/bank)
3. **Withdrawal**: Create endpoint to withdraw funds to bank account
4. **Transaction Details**: Get detailed transaction information
5. **Statements**: Generate account statements
6. **Alerts**: Set up transaction alerts/notifications
7. **Dispute Handling**: Implement transaction dispute workflow

---

Generated: April 19, 2026
Version: 1.0
