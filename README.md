# TransactIQ Backend

A comprehensive Spring Boot REST API for financial transaction management with role-based access control, risk scoring, and automated payment approval workflows.

## 🎯 Features

- **User Authentication & Authorization**: JWT-based authentication with role-based access control (USER, CHECKER, ADMIN)
- **Account Management**: Multi-currency account support (USD, CAD, EUR)
- **Payment Processing**: Internal and external transfers with automatic currency conversion
- **Risk Scoring**: Automatic risk calculation for payments with auto-approval for low-risk transactions
- **Checker Workflow**: Approval queue for checkers with risk-based prioritization
- **Exchange Rates**: Support for international transfers with automatic currency conversion
- **n8n Integration**: Webhook notifications for payment approvals

## 🏗️ Technology Stack

### Core Technologies
- **Java 17** - Programming language
- **Spring Boot 3.2.0** - Framework
- **Maven** - Build tool and dependency management
- **PostgreSQL 15** - Database
- **Docker & Docker Compose** - Database containerization

### Key Dependencies
- **Spring Web** - REST API endpoints
- **Spring Data JPA** - Database persistence
- **Spring Security** - Authentication and authorization
- **JJWT 0.12.3** - JWT token generation and validation
- **BCrypt** - Password hashing
- **Lombok** - Code simplification
- **Hibernate** - ORM framework

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Docker Desktop (for PostgreSQL)
- Maven (or use Maven wrapper `mvnw.cmd`)

### Step 1: Start PostgreSQL

**Using Docker (Recommended):**

1. Start Docker Desktop
2. Run:
   ```powershell
   docker-compose up -d
   ```
   Or use the provided script:
   ```powershell
   .\start-postgres.ps1
   ```

3. Verify it's running:
   ```powershell
   docker ps
   ```

**Database Credentials:**
- Host: `localhost:5432`
- Database: `transactiq_db`
- Username: `postgres`
- Password: `postgres`

### Step 2: Start the Backend

```powershell
.\mvnw.cmd spring-boot:run
```

Wait for:
```
Tomcat started on port(s): 8080 (http)
```

### Step 3: Test the Backend

**Health Check:**
```powershell
curl http://localhost:8080/api/health
```

Or open in browser: `http://localhost:8080/api/health`

## 📁 Project Structure

```
TransactIQ-backend/
├── src/main/java/com/transactiq/backend/
│   ├── config/              # Configuration classes
│   │   ├── CorsConfig.java          # Global CORS configuration
│   │   ├── SecurityConfig.java      # Spring Security & JWT setup
│   │   ├── RestTemplateConfig.java  # RestTemplate bean
│   │   └── ExchangeRateInitializer.java  # Default exchange rates
│   ├── controller/          # REST API endpoints
│   │   ├── AccountController.java   # Account management APIs
│   │   ├── AuthController.java      # Authentication APIs
│   │   ├── DashboardController.java # Dashboard data APIs
│   │   ├── PaymentController.java   # Payment management APIs
│   │   ├── ExchangeRateController.java  # Exchange rate APIs
│   │   ├── CheckerDashboardController.java  # Checker dashboard APIs
│   │   ├── AdminController.java     # Admin panel APIs
│   │   └── HealthController.java    # Health check endpoint
│   ├── entity/              # Database entities
│   │   ├── Account.java             # Account entity
│   │   ├── Payment.java             # Payment entity
│   │   ├── User.java                # User entity with roles
│   │   └── ExchangeRate.java       # Exchange rate entity
│   ├── repository/           # Data access layer
│   │   ├── AccountRepository.java
│   │   ├── PaymentRepository.java
│   │   ├── UserRepository.java
│   │   └── ExchangeRateRepository.java
│   ├── service/             # Business logic layer
│   │   ├── AccountService.java
│   │   ├── PaymentService.java
│   │   ├── ExchangeRateService.java
│   │   ├── RiskScoreService.java    # Risk calculation
│   │   └── N8nNotifier.java         # n8n webhook integration
│   ├── filter/              # Security filters
│   │   └── JwtAuthenticationFilter.java
│   ├── util/                # Utility classes
│   │   ├── JwtUtil.java            # JWT operations
│   │   ├── RoleUtil.java           # Role-based access helpers
│   │   └── SecurityUtil.java       # Security context helpers
│   └── TransactIqBackendApplication.java
├── src/main/resources/
│   └── application.yml      # Application configuration
├── docker-compose.yml       # PostgreSQL container setup
├── init.sql                 # Database initialization
├── pom.xml                  # Maven dependencies
└── README.md
```

## 🗄️ Database Schema

### Users Table
- `id` (BIGINT, PRIMARY KEY)
- `username` (VARCHAR, UNIQUE, NOT NULL)
- `email` (VARCHAR, UNIQUE, NOT NULL)
- `password` (VARCHAR, NOT NULL) - BCrypt hashed
- `first_name` (VARCHAR)
- `last_name` (VARCHAR)
- `role` (VARCHAR/ENUM) - `USER`, `CHECKER`, `ADMIN` (default: `USER`)
- `is_active` (BOOLEAN, default: true)
- `created_at` (TIMESTAMP)

### Accounts Table
- `id` (BIGINT, PRIMARY KEY)
- `account_number` (VARCHAR, UNIQUE, NOT NULL) - Auto-generated
- `account_type` (VARCHAR, NOT NULL) - `CHECKING`, `SAVINGS`, `BUSINESS`
- `balance` (DECIMAL, NOT NULL, default: 0.00)
- `currency` (VARCHAR(3), NOT NULL) - `USD`, `CAD`, `EUR` (default: `USD`)
- `is_active` (BOOLEAN, default: true)
- `user_id` (BIGINT, FOREIGN KEY → users.id)
- `created_at` (TIMESTAMP)

### Payments Table
- `id` (BIGINT, PRIMARY KEY)
- `transaction_id` (VARCHAR, UNIQUE, NOT NULL) - Auto-generated
- `amount` (DECIMAL, NOT NULL, min: 0.01)
- `currency` (VARCHAR(3), NOT NULL) - From account currency
- `converted_amount` (DECIMAL) - Converted amount for different currency
- `converted_currency` (VARCHAR(3)) - To account currency
- `exchange_rate` (DECIMAL(19, 6)) - Exchange rate used
- `status` (VARCHAR) - `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`
- `transfer_type` (VARCHAR) - `INTERNAL`, `EXTERNAL`
- `risk_score` (DECIMAL(5,2)) - Risk score 0-100
- `risk_level` (VARCHAR) - `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`
- `auto_approved` (BOOLEAN) - True if auto-approved
- `description` (VARCHAR)
- `from_account_id` (BIGINT, FOREIGN KEY → accounts.id)
- `to_account_id` (BIGINT, FOREIGN KEY → accounts.id)
- `created_at` (TIMESTAMP)
- `approved_at` (TIMESTAMP)
- `approved_by` (BIGINT, FOREIGN KEY → users.id)

### Exchange Rates Table
- `id` (BIGSERIAL, PRIMARY KEY)
- `from_currency` (VARCHAR(3), NOT NULL)
- `to_currency` (VARCHAR(3), NOT NULL)
- `rate` (DECIMAL(19, 6), NOT NULL)
- `is_active` (BOOLEAN, default: true)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- UNIQUE(from_currency, to_currency)

## 🔐 Authentication & Authorization

### User Roles

1. **USER** (Default)
   - Create and view own accounts
   - Create payments
   - View own payment history
   - Cannot approve/reject payments

2. **CHECKER**
   - All USER permissions
   - View approval queue
   - View risk statistics
   - Approve/reject payments
   - Cannot manage users

3. **ADMIN**
   - All CHECKER permissions
   - Manage users (view, update roles)
   - View all accounts
   - System administration

### JWT Authentication

All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

## 📡 API Endpoints

### Authentication

#### `POST /api/auth/register`
Register a new user.

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "id": 1,
  "email": "john@example.com",
  "username": "johndoe",
  "role": "user",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### `POST /api/auth/login`
Login with email and password.

**Request:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "id": 1,
  "email": "john@example.com",
  "username": "johndoe",
  "role": "user",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### `GET /api/auth/me`
Get current authenticated user details.

**Headers:** `Authorization: Bearer <token>`

### Accounts

#### `GET /api/accounts`
Get all accounts for the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
[
  {
    "id": 1,
    "accountNumber": "ACC123456789",
    "accountType": "CHECKING",
    "balance": 5000.00,
    "currency": "USD",
    "isActive": true,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

#### `POST /api/accounts`
Create a new account.

**Request:**
```json
{
  "name": "Savings Account",
  "type": "savings",
  "currency": "USD",
  "balance": 1000.00
}
```

#### `GET /api/accounts/search?q={query}`
Search for external accounts (for external transfers).

**Query Parameters:**
- `q` (required): Search query (minimum 3 characters)

### Payments

#### `GET /api/payments`
Get all payments for the authenticated user (or all payments for CHECKER/ADMIN).

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
[
  {
    "id": 1,
    "transactionId": "TXN123456789",
    "amount": 100.00,
    "currency": "USD",
    "status": "APPROVED",
    "riskScore": 5.00,
    "riskLevel": "LOW",
    "autoApproved": true,
    "transferType": "internal",
    "fromAccount": { ... },
    "toAccount": { ... },
    "conversion": { ... },
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

#### `POST /api/payments`
Create a new payment.

**Request:**
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 100.00,
  "currency": "USD",
  "description": "Payment description",
  "transferType": "internal"
}
```

**Response:**
```json
{
  "id": 1,
  "amount": 100.00,
  "currency": "USD",
  "status": "PENDING",
  "riskScore": 5.00,
  "riskLevel": "LOW",
  "autoApproved": false,
  "conversion": null
}
```

#### `PUT /api/payments/{id}/approve`
Approve a payment (CHECKER/ADMIN only).

**Headers:** `Authorization: Bearer <token>`

#### `PUT /api/payments/{id}/reject`
Reject a payment (CHECKER/ADMIN only).

**Headers:** `Authorization: Bearer <token>`

### Dashboard

#### `GET /api/dashboard`
Get dashboard data for the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "totalBalance": 10000.00,
  "recentTransactions": 10,
  "activeAccounts": 2,
  "pendingPayments": 3,
  "approvedPayments": 7,
  "currencyBalances": [
    {
      "currency": "USD",
      "balance": 8000.00,
      "percentage": 80.0
    },
    {
      "currency": "CAD",
      "balance": 2000.00,
      "percentage": 20.0
    }
  ]
}
```

### Checker Dashboard (CHECKER/ADMIN only)

#### `GET /api/checker/approval-queue`
Get pending payments sorted by risk score.

**Headers:** `Authorization: Bearer <token>`

#### `GET /api/checker/risk-statistics`
Get risk statistics.

**Headers:** `Authorization: Bearer <token>`

#### `GET /api/checker/dashboard`
Get complete checker dashboard.

**Headers:** `Authorization: Bearer <token>`

### Admin Panel (ADMIN only)

#### `GET /api/admin/users`
Get all users.

**Headers:** `Authorization: Bearer <token>`

#### `PUT /api/admin/users/{userId}/role`
Update user role.

**Request:**
```json
{
  "role": "checker"
}
```

#### `GET /api/admin/dashboard`
Get admin dashboard.

**Headers:** `Authorization: Bearer <token>`

### Exchange Rates

#### `GET /api/exchange-rates`
Get all active exchange rates.

**Headers:** `Authorization: Bearer <token>`

#### `GET /api/exchange-rates/convert?amount={amount}&fromCurrency={from}&toCurrency={to}`
Convert amount between currencies.

**Query Parameters:**
- `amount` (required): Amount to convert
- `fromCurrency` (required): Source currency code
- `toCurrency` (required): Target currency code

### Health Check

#### `GET /api/health`
Health check endpoint (no authentication required).

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-01-01T00:00:00"
}
```

## 🎲 Risk Scoring & Auto-Approval

### Risk Score Calculation

The system automatically calculates a risk score (0-100) for each payment based on:

1. **Amount Risk** (0-30 points): Higher amounts = higher risk
2. **Currency Risk** (0-20 points): International transfers = higher risk
3. **Transfer Type Risk** (0-15 points): External transfers = higher risk
4. **Time Risk** (0-10 points): Unusual hours (2 AM - 6 AM) = higher risk
5. **Balance Risk** (0-15 points): Low remaining balance = higher risk
6. **History Risk** (0-10 points): First-time recipients = higher risk

### Risk Levels

- **LOW** (0-30): Auto-approved if amount is small
- **MEDIUM** (31-60): Requires manual approval
- **HIGH** (61-80): Requires manual approval, priority review
- **VERY_HIGH** (81-100): Requires manual approval, urgent review

### Auto-Approval Rules

Payments are automatically approved if:
- Risk score ≤ 20 AND amount ≤ $10,000
- OR Risk score ≤ 30 AND amount ≤ $1,000

**Examples:**
- $10 payment → Risk: 0 → Auto-approved ✅
- $500 payment → Risk: 0 → Auto-approved ✅
- $10,000 international transfer → Risk: 40+ → PENDING (manual approval) ⚠️

## 💱 Exchange Rates

### Supported Currencies
- USD (US Dollar)
- CAD (Canadian Dollar)
- EUR (Euro)

### Default Exchange Rates
The system initializes with default exchange rates on startup:
- USD → CAD: 1.350000
- USD → EUR: 0.920000
- CAD → USD: 0.740741
- EUR → USD: 1.086957

### Currency Conversion

When creating a payment between accounts with different currencies:
1. System detects currency difference
2. Looks up exchange rate from database
3. Calculates converted amount
4. Stores both original and converted amounts
5. On approval, deducts original amount and adds converted amount

**Example:**
- Transfer $100 USD to CAD account
- Exchange Rate: 1 USD = 1.35 CAD
- Deducts: $100 USD
- Adds: $135 CAD

## 🔔 n8n Webhook Integration

The system sends webhook notifications to n8n when payments are approved.

**Webhook URL:** `https://ancjainil.app.n8n.cloud/webhook/webhook/payment_approved`

**Payload:**
```json
{
  "transactionId": "TXN123456789",
  "amount": 100.00,
  "currency": "USD",
  "approvedBy": "admin",
  "approvedAt": "2025-01-01T00:00:00",
  "toEmail": "recipient@example.com"
}
```

**Note:** Auto-approved payments do not trigger n8n notifications.

## 🔧 Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/transactiq_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  security:
    jwt:
      secret: your-secret-key-here
      expiration: 86400000  # 24 hours

server:
  port: 8080

cors:
  allowed-origins: http://localhost:5173
```

### CORS Configuration

The backend is configured to allow requests from:
- Origin: `http://localhost:5173` (React frontend)
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Headers: Authorization, Content-Type
- Credentials: Supported

## 🧪 Testing

### Test Health Endpoint
```powershell
curl http://localhost:8080/api/health
```

### Test Registration
```powershell
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"password123"}'
```

### Test Login
```powershell
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### Test with Authentication
```powershell
curl http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <token>"
```

## 🐳 Docker Commands

### Start PostgreSQL
```powershell
docker-compose up -d
```

### Stop PostgreSQL
```powershell
docker-compose down
```

### View Logs
```powershell
docker-compose logs postgres
```

### Remove All Data
```powershell
docker-compose down -v
```

## 🚨 Troubleshooting

### Port 5432 already in use
- Check if another PostgreSQL instance is running
- Stop it or change port in `docker-compose.yml`

### Connection refused
- Make sure PostgreSQL is running
- Check Docker Desktop is started
- Verify database credentials in `application.yml`

### JWT token expired
- Tokens expire after 24 hours
- Login again to get a new token

### Payment not auto-approved
- Check risk score (should be ≤ 30 for small amounts)
- Verify sufficient balance in from account
- Check payment status in database

## 📝 Environment Variables

You can override configuration using environment variables:

```powershell
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_secret_key"
```

## 🔒 Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Tokens**: Secure token-based authentication
- **Role-Based Access Control**: USER, CHECKER, ADMIN roles
- **CORS Protection**: Configured for specific origins
- **Input Validation**: Request validation using Jakarta Validation
- **SQL Injection Protection**: JPA/Hibernate parameterized queries

## 📚 Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/
- **JWT Documentation**: https://jwt.io/

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Commit and push
5. Create a pull request

## 📄 License

This project is part of the TransactIQ application.

---

**Built with ❤️ using Spring Boot**
