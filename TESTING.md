# Testing the TransactIQ Backend

## Prerequisites

1. **Java 21** (or Java 17+)
   - Check version: `java -version`
   - Download from: https://adoptium.net/

2. **Maven**
   - Download from: https://maven.apache.org/download.cgi
   - Or use IDE (IntelliJ IDEA, Eclipse, VS Code) with Maven support

3. **PostgreSQL Database** (optional for basic testing)
   - Install PostgreSQL: https://www.postgresql.org/download/
   - Create database: `CREATE DATABASE transactiq_db;`
   - Or use H2 in-memory database for testing (modify `application.yml`)

## Running the Application

### Option 1: Using Maven Command Line

```bash
# Build and run
mvn spring-boot:run

# Or build first, then run
mvn clean package
java -jar target/transactiq-backend-1.0.0.jar
```

### Option 2: Using IDE

1. Open the project in IntelliJ IDEA, Eclipse, or VS Code
2. Ensure Java 21 is configured
3. Run `TransactIqBackendApplication.java` as a Java application

### Option 3: Using Maven Wrapper (if available)

```bash
./mvnw spring-boot:run          # Linux/Mac
.\mvnw.cmd spring-boot:run      # Windows
```

## Testing Endpoints

Once the application is running on `http://localhost:8080`, you can test:

### 1. Health Check (No database required)
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T12:00:00",
  "service": "TransactIQ Backend"
}
```

### 2. Test CORS from React Frontend

From your React app running on `http://localhost:5173`:

```javascript
// Test health endpoint
fetch('http://localhost:8080/api/health', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
  },
  credentials: 'include'
})
.then(response => response.json())
.then(data => console.log('Health check:', data))
.catch(error => console.error('Error:', error));

// Test account endpoint (requires database)
fetch('http://localhost:8080/api/accounts/user/1', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_TOKEN' // if JWT is implemented
  },
  credentials: 'include'
})
.then(response => response.json())
.then(data => console.log('Accounts:', data))
.catch(error => console.error('Error:', error));
```

### 3. Using Postman or cURL

```bash
# Health check
curl -X GET http://localhost:8080/api/health

# Get accounts (requires user ID)
curl -X GET http://localhost:8080/api/accounts/user/1

# Create account (requires user ID)
curl -X POST http://localhost:8080/api/accounts?userId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "accountType": "CHECKING",
    "balance": 1000.00,
    "currency": "USD"
  }'
```

## CORS Testing

To verify CORS is working:

1. Open browser console on `http://localhost:5173`
2. Make a request to `http://localhost:8080/api/health`
3. Check Network tab - should see CORS headers:
   - `Access-Control-Allow-Origin: http://localhost:5173`
   - `Access-Control-Allow-Credentials: true`
   - `Access-Control-Expose-Headers: Authorization`

## Troubleshooting

### Port 8080 already in use
- Change port in `application.yml`: `server.port: 8081`

### Database connection error
- Ensure PostgreSQL is running
- Check database credentials in `application.yml`
- Or temporarily use H2 database for testing

### Maven not found
- Install Maven or use IDE with Maven support
- Or download Maven wrapper: `mvn wrapper:wrapper`


