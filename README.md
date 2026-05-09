# SecurePay Core

SecurePay Core is a distributed digital payments platform built with Spring Boot microservices. It demonstrates a UPI-like payment flow with saga-based transaction orchestration, compensating reversals, idempotent retries, and Docker-based internal service isolation.

## Services

- `user-service`: Public-facing user and payment initiation gateway.
- `payment-service`: Internal transaction orchestrator and saga engine.
- `ledger-service`: Internal ledger and balance management service.
- `notification-service`: Internal notification event handler.
- `mysql`: Shared MySQL database backend.

## Architecture

- Spring Boot MVC, Spring Data JPA, Spring Security
- REST APIs with JSON
- Microservices with internal/external Docker networks
- Saga-based transaction orchestration with idempotent state transitions
- Compensating reversal support for partial failures

## Prerequisites

Before running SecurePay Core, ensure you have the following installed:

- **Java 11+**: [Download](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.8+**: [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose**: [Download Docker Desktop](https://www.docker.com/products/docker-desktop)

Verify installation:
```bash
java -version
mvn -version
docker --version
docker compose --version
```

## Quick Start



### Manual Build

1. Build each service:
   ```bash
   cd user-service && mvn clean package -DskipTests && cd ..
   cd payment-service && mvn clean package -DskipTests && cd ..
   cd ledger-service && mvn clean package -DskipTests && cd ..
   cd notification-service && mvn clean package -DskipTests && cd ..
   ```

2. Start Docker Compose:
   ```bash
   docker compose up --build
   ```

3. User service will be available at **http://localhost:8080**

## Accessing the Services

- **User Service (Public Gateway)**: http://localhost:8080
- **Payment Service**: Internal only (not externally accessible)
- **Ledger Service**: Internal only (not externally accessible)
- **Notification Service**: Internal only (not externally accessible)
- **MySQL**: localhost:3307 (root/securepay)

## Database

All services share a single MySQL database (v8.0) with separate schemas:
- `userdb`: User service data
- `paymentdb`: Payment transactions
- `ledgerdb`: Account balances and ledger
- `notificationdb`: Notification logs

**MySQL Connection Details**:
- Host: `mysql` (internal) or `localhost` (external)
- Port: `3307`
- Username: `root`
- Password: `securepay`

## Troubleshooting

### Issue: Maven build fails with "mvn: command not found"
**Solution**: Ensure Maven is installed and in your system PATH. Check: `mvn -version`

### Issue: Docker build fails
**Solution**: Ensure Docker daemon is running. Start Docker Desktop and try again.

### Issue: "Cannot connect to MySQL" error
**Solution**: Verify MySQL container is running:
```bash
docker ps | grep mysql
```
If not running, restart containers:
```bash
docker compose down
docker compose up --build
```

### Issue: Services fail to start
**Solution**: Check service logs:
```bash
docker compose logs <service-name>
```

### Issue: Port already in use
**Solution**: Change port mappings in `docker-compose.yml` or kill the process using the port.

## Notes

- Only `user-service` is exposed to the external `public` network.
- `payment-service`, `ledger-service`, and `notification-service` are attached only to an internal Docker network.
- The system uses a shared secret (`secret-key`) for internal requests and HTTP Basic auth for the public gateway.
- Data persists in the `mysql-data` volume between container restarts.
