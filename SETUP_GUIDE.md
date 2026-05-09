# SecurePayCore Setup Guide

This guide provides step-by-step instructions to get SecurePayCore running on your machine.

## System Requirements

- **OS**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **RAM**: Minimum 4GB, recommended 8GB+
- **Disk Space**: 2GB free space

## Step 1: Install Prerequisites

### Java Installation

1. Download Java 11 or higher:
   - Visit https://www.oracle.com/java/technologies/downloads/
   - Download JDK (not JRE)

2. Install and verify:
   ```bash
   # On Windows
   java -version
   
   # You should see output like: "java version "11.0.x" ..."
   ```

### Maven Installation

1. Download Maven 3.8+:
   - Visit https://maven.apache.org/download.cgi
   - Download the binary zip

2. Extract and add to PATH:
   - **Windows**: Extract to `C:\tools\maven` and add to system PATH
   - **Linux/Mac**: Extract to `~/tools/maven` and add to `.bashrc` or `.zshrc`

3. Verify:
   ```bash
   mvn -version
   
   # You should see Maven version and Java version
   ```

### Docker Installation

1. Download Docker Desktop:
   - Visit https://www.docker.com/products/docker-desktop
   - Download and install for your OS

2. Start Docker Desktop (important on Windows/Mac)

3. Verify:
   ```bash
   docker --version
   docker compose --version
   ```

## Step 2: Clone the Repository

```bash
# Clone the repository
git clone <repository-url>
cd SecurePayCore
```

## Step 3: Build All Services

### Manual Build

```bash
# Build user-service
cd user-service
mvn clean package -DskipTests
cd ..

# Build payment-service
cd payment-service
mvn clean package -DskipTests
cd ..

# Build ledger-service
cd ledger-service
mvn clean package -DskipTests
cd ..

# Build notification-service
cd notification-service
mvn clean package -DskipTests
cd ..
```

## Step 4: Start the Application

```bash
# Make sure you're in the SecurePayCore root directory
docker compose up --build
```

You should see output showing:
- MySQL container starting
- All four services building and starting
- Health checks passing

## Step 5: Verify Everything is Running

1. **Check containers**:
   ```bash
   docker ps
   ```
   You should see 5 containers: mysql, user-service, payment-service, ledger-service, notification-service

2. **Access the application**:
   - Open http://localhost:8080 in your browser
   - Or use curl:
     ```bash
     curl http://localhost:8080
     ```

3. **Check service logs**:
   ```bash
   # View logs for a specific service
   docker compose logs user-service
   
   # View logs for all services
   docker compose logs
   
   # Follow logs in real-time
   docker compose logs -f
   ```

## Available Endpoints

### User Service (Public - Port 8080)
- `GET /health` - Health check
- `POST /users` - Create user
- `POST /initiate-payment` - Initiate payment

### Payment Service (Internal - Port 8081)
- Orchestrates payment saga
- Communicates with ledger and notification services

### Ledger Service (Internal - Port 8082)
- Manages account balances
- Updates ledger entries

### Notification Service (Internal - Port 8083)
- Sends transaction notifications
- Logs notification events

### MySQL (Port 3307)
- Connect with: `mysql -h localhost -P 3307 -u root -p`
- Password: `securepay`

## Stopping the Application

```bash
# Stop all containers (data persists)
docker compose down

# Stop and remove volumes (removes data)
docker compose down -v
```

## Common Issues and Solutions

### Issue: Build Process is Slow

**Cause**: First build downloads all dependencies
**Solution**: This is normal, subsequent builds will be faster. Maven caches dependencies locally.

### Issue: "Cannot connect to Docker daemon"

**Windows/Mac Solution**:
- Open Docker Desktop application
- It must be running to use Docker commands

**Linux Solution**:
- Ensure Docker service is running: `sudo systemctl start docker`

### Issue: "Port 8080 is already in use"

**Solution**: Either:
1. Stop the process using port 8080: `lsof -i :8080` (Linux/Mac) or `netstat -ano` (Windows)
2. Or change the port in `docker-compose.yml`: Change `8080:8080` to `8888:8080`

### Issue: Services won't start, memory error

**Cause**: Not enough RAM allocated to Docker
**Solution**:
- Docker Desktop Settings → Resources
- Increase Memory to 4GB minimum
- Increase to 6-8GB if available

### Issue: "Maven is not installed"

**Solution**:
- Add Maven `bin` directory to system PATH
- Restart terminal/command prompt after adding PATH

### Database Connection Error

**Solution**: Verify MySQL is running:
```bash
docker exec securepay-mysql mysql -u root -psecurepay -e "SELECT 1"
```

If this returns `ERROR`, restart Docker:
```bash
docker compose restart mysql
```

## Testing the Deployment

### Application Readiness

Once all containers show as *Running*, make an HTTP request:

```bash
# Test user-service health
curl http://localhost:8080/health

# Expected response:
# {"status":"UP"}
```

### Database Connectivity

```bash
# Check if MySQL is accepting connections
docker exec securepay-mysql mysql -u root -psecurepay -e "SHOW DATABASES;"
```

You should see databases:
- userdb
- paymentdb
- ledgerdb
- notificationdb

## Next Steps

- Review [API_REFERENCE.md](API_REFERENCE.md) for available endpoints
- Check [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for architecture details
- Explore the service source code in respective directories

## Getting Help

- Check Docker logs: `docker compose logs <service-name>`
- Review [troubleshooting section](README.md#troubleshooting) in README
- Verify all prerequisites are correctly installed

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                 Client (Browser)                 │
├─────────────────────────────────────────────────┤
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │ User Service (port 8080)                 │   │
│  │ - Public API                             │   │
│  │ - HTTP Basic Auth                        │   │
│  └──────────────┬───────────────────────────┘   │
└─────────────────┼──────────────────────────────┘
                  │ Internal Network
        ┌─────────┴─────────┬──────────────┐
        │                   │              │
   ░░░░░░░░░           ░░░░░░░░░      ░░░░░░░░░
  ░ Payment ░         ░ Ledger░      ░Notif. ░
  ░ Service ░────────░ Service░     ░Service░
   ░░░░░░░░░          ░░░░░░░░░      ░░░░░░░░░
        │                   │              │
        └─────────┬─────────┴──────────────┘
                  │
            ┌─────▼────────┐
            │ MySQL (8.0)  │
            │ - Shared DB  │
            │ - 4 schemas  │
            └──────────────┘
```

## Performance Tips

1. **Allocate sufficient RAM**: 4GB minimum, 6-8GB recommended
2. **Use SSD**: Faster I/O improves build and startup times
3. **Network**: Ensure stable internet connection for dependency downloads
4. **Background processes**: Close unnecessary applications to free resources
