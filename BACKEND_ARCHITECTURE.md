# TransactIQ Backend - Architecture & Flow Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Layered Architecture](#layered-architecture)
4. [Component Details](#component-details)
5. [Request Flow](#request-flow)
6. [Authentication & Authorization Flow](#authentication--authorization-flow)
7. [Payment Flow](#payment-flow)
8. [Exchange Rate Flow](#exchange-rate-flow)
9. [Database Schema](#database-schema)
10. [Security Architecture](#security-architecture)

---

## Architecture Overview

TransactIQ backend follows a **layered architecture (3-tier architecture)** pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer (Controllers)       │
│   AccountController, PaymentController, etc.     │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│            Business Logic Layer (Services)       │
│   AccountService, PaymentService, etc.           │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│          Data Access Layer (Repositories)       │
│   AccountRepository, PaymentRepository, etc.     │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│              Database (PostgreSQL)                │
└───────────────────────────────────────────────────┘
```

---

## Technology Stack

### Core Framework
- **Spring Boot 3.x** - Main application framework
- **Java 17** - Programming language
- **Maven** - Build tool and dependency management

### Database & Persistence
- **PostgreSQL 15** - Relational database
- **Spring Data JPA** - Data access abstraction
- **Hibernate** - ORM (Object-Relational Mapping)
- **JPA/Hibernate** - Automatic schema management (`ddl-auto: update`)

### Security
- **Spring Security** - Authentication and authorization framework
- **JWT (JSON Web Tokens)** - Token-based authentication
- **BCrypt** - Password hashing
- **JJWT 0.12.3** - JWT library

### Additional Libraries
- **Lombok** - Reduces boilerplate code
- **Validation API** - Input validation
- **Spring DevTools** - Development utilities

---

## Layered Architecture

### 1. **Controller Layer** (Presentation)
**Location:** `com.transactiq.backend.controller.*`

**Purpose:** 
- Handle HTTP requests/responses
- Validate request data
- Format responses
- Delegate business logic to services

**Controllers:**
- `AccountController` - Account management (GET, POST accounts)
- `PaymentController` - Payment operations (create, approve, reject, list)
- `AuthController` - Authentication (login, register, get current user)
- `DashboardController` - Dashboard data aggregation
- `ExchangeRateController` - Exchange rate management
- `HealthController` - Health check endpoint

**Responsibilities:**
- ✅ Receive HTTP requests
- ✅ Parse request body/parameters
- ✅ Call appropriate service methods
- ✅ Format response data
- ✅ Handle HTTP status codes
- ✅ Return JSON responses

---

### 2. **Service Layer** (Business Logic)
**Location:** `com.transactiq.backend.service.*`

**Purpose:**
- Implement business rules
- Orchestrate data operations
- Handle transactions
- Validate business logic

**Services:**
- `AccountService` - Account business logic
- `PaymentService` - Payment processing, currency conversion, fund transfers
- `ExchangeRateService` - Exchange rate calculations and conversions

**Responsibilities:**
- ✅ Business logic validation
- ✅ Transaction management (`@Transactional`)
- ✅ Data transformation
- ✅ Orchestrate multiple repository calls
- ✅ Handle complex business rules

---

### 3. **Repository Layer** (Data Access)
**Location:** `com.transactiq.backend.repository.*`

**Purpose:**
- Database operations
- Query execution
- Entity persistence
- Data retrieval

**Repositories:**
- `AccountRepository` - Account CRUD operations
- `PaymentRepository` - Payment CRUD operations
- `UserRepository` - User CRUD operations
- `ExchangeRateRepository` - Exchange rate operations

**Responsibilities:**
- ✅ CRUD operations (Create, Read, Update, Delete)
- ✅ Custom queries (JPQL, native SQL)
- ✅ Data filtering and searching
- ✅ Database interaction abstraction

---

### 4. **Entity Layer** (Domain Models)
**Location:** `com.transactiq.backend.entity.*`

**Purpose:**
- Represent database tables
- Define relationships
- Business domain models

**Entities:**
- `User` - User accounts with roles (USER, CHECKER, ADMIN)
- `Account` - Bank accounts with currency support
- `Payment` - Payment transactions with transfer types
- `ExchangeRate` - Currency exchange rates

**Responsibilities:**
- ✅ Map to database tables
- ✅ Define entity relationships (OneToMany, ManyToOne)
- ✅ Validation constraints
- ✅ Business domain representation

---

## Component Details

### Configuration Layer
**Location:** `com.transactiq.backend.config.*`

1. **SecurityConfig**
   - Spring Security configuration
   - JWT filter integration
   - CORS configuration
   - Protected/public endpoints
   - Password encoder (BCrypt)

2. **CorsConfig**
   - Global CORS policy
   - Allows requests from `http://localhost:5173`
   - Configures allowed methods, headers, credentials

3. **ExchangeRateInitializer**
   - Initializes default exchange rates on startup
   - Implements `CommandLineRunner`

---

### Filter Layer
**Location:** `com.transactiq.backend.filter.*`

**JwtAuthenticationFilter**
- Intercepts all requests
- Extracts JWT token from Authorization header
- Validates token
- Sets authentication in SecurityContext
- Allows/denies request based on token validity

---

### Utility Layer
**Location:** `com.transactiq.backend.util.*`

1. **JwtUtil**
   - Generate JWT tokens
   - Validate JWT tokens
   - Extract claims from tokens

2. **SecurityUtil**
   - Extract current user ID from SecurityContext
   - Helper for authentication information

3. **RoleUtil**
   - Role-based access control checks
   - `canApprovePayments()`, `isAdmin()`, `isChecker()`
   - Role formatting utilities

---

## Request Flow

### Standard Request Flow (Authenticated)

```
1. HTTP Request
   ↓
2. CORS Filter (CorsConfig)
   ↓
3. JWT Authentication Filter (JwtAuthenticationFilter)
   ├─ Extract token from "Authorization: Bearer <token>" header
   ├─ Validate token (JwtUtil)
   ├─ Extract user ID from token
   └─ Set authentication in SecurityContext
   ↓
4. Spring Security Filter Chain
   ├─ Check if endpoint requires authentication
   ├─ Validate user permissions (if needed)
   └─ Allow/deny request
   ↓
5. Controller Layer
   ├─ Receive HTTP request
   ├─ Parse request body/parameters
   ├─ Validate request data
   └─ Call Service Layer
   ↓
6. Service Layer
   ├─ Business logic validation
   ├─ Call Repository Layer
   ├─ Process data
   └─ Return result
   ↓
7. Repository Layer
   ├─ Execute database queries
   ├─ Return entity objects
   └─ Handle transactions
   ↓
8. Service Layer (cont.)
   ├─ Process repository results
   ├─ Apply business rules
   └─ Return to Controller
   ↓
9. Controller Layer (cont.)
   ├─ Format response data
   ├─ Set HTTP status code
   └─ Return JSON response
   ↓
10. HTTP Response
```

---

## Authentication & Authorization Flow

### Registration Flow

```
1. POST /api/auth/register
   Request: { name, email, password }
   ↓
2. AuthController.register()
   ├─ Validate input (email, password)
   ├─ Check if email/username exists
   └─ Generate username from email
   ↓
3. Hash password (BCrypt)
   ↓
4. Create User entity
   ├─ Set role = USER (default)
   ├─ Set isActive = true
   └─ Save to database
   ↓
5. Generate JWT token (JwtUtil)
   ├─ Include user ID and email
   ├─ Set expiration (24 hours)
   └─ Sign with secret key
   ↓
6. Return Response
   {
     "token": "eyJhbGc...",
     "user": {
       "id": 1,
       "email": "user@example.com",
       "name": "John Doe",
       "role": "user"
     }
   }
```

### Login Flow

```
1. POST /api/auth/login
   Request: { email, password }
   ↓
2. AuthController.login()
   ├─ Find user by email (UserRepository)
   └─ Validate password (BCrypt.matches())
   ↓
3. If password matches:
   ├─ Generate JWT token
   └─ Return token + user data
   ↓
4. If password invalid:
   └─ Return 401 Unauthorized
```

### JWT Token Validation Flow

```
1. Request arrives with: "Authorization: Bearer <token>"
   ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ├─ Extract token from header
   ├─ Validate token signature (JwtUtil)
   ├─ Check expiration
   ├─ Extract user ID from claims
   └─ Set authentication in SecurityContext
   ↓
3. SecurityContext contains:
   ├─ Principal: User ID (Long)
   ├─ Authenticated: true
   └─ Authorities: (empty for now)
   ↓
4. Controller can access user ID:
   └─ SecurityUtil.getCurrentUserId()
```

### Role-Based Access Control (RBAC)

```
1. User has role: USER, CHECKER, or ADMIN
   ↓
2. PaymentController.approvePayment()
   ├─ Get current user (UserRepository)
   ├─ Check role (RoleUtil.canApprovePayments())
   └─ If not CHECKER/ADMIN → Return 403 Forbidden
   ↓
3. RoleUtil.canApprovePayments()
   ├─ Check if user.role == CHECKER or ADMIN
   └─ Return true/false
```

---

## Payment Flow

### Payment Creation Flow (Internal Transfer)

```
1. POST /api/payments
   Request: {
     fromAccountId: 1,
     toAccountId: 2,
     amount: 100.00,
     description: "Transfer"
   }
   ↓
2. PaymentController.createPayment()
   ├─ Validate request data
   ├─ Get fromAccount (must belong to user)
   ├─ Get toAccount
   └─ Call PaymentService
   ↓
3. PaymentService.createPayment()
   ├─ Detect transfer type (INTERNAL/EXTERNAL)
   ├─ Validate accounts (active, ownership)
   ├─ Check currency conversion needed
   ├─ Calculate exchange rate (if different currencies)
   ├─ Calculate converted amount
   ├─ Set payment status = PENDING
   └─ Save to database
   ↓
4. PaymentRepository.save()
   ├─ Generate transaction ID
   ├─ Store payment with:
   │  ├─ amount (original)
   │  ├─ currency (fromAccount currency)
   │  ├─ convertedAmount (if needed)
   │  ├─ convertedCurrency (if needed)
   │  ├─ exchangeRate
   │  └─ status = PENDING
   └─ Return saved payment
   ↓
5. Response
   {
     "id": 1,
     "amount": 100.00,
     "currency": "USD",
     "status": "PENDING",
     "conversion": { ... } // if currencies differ
   }
```

### Payment Creation Flow (External Transfer)

```
1. POST /api/payments
   Request: {
     fromAccountId: 1,
     toAccountId: 123, // Different user's account
     amount: 100.00,
     transferType: "external"
   }
   ↓
2. PaymentService.createPayment()
   ├─ Detect: toAccount belongs to different user
   ├─ Set transferType = EXTERNAL
   ├─ Validate transfer type (if provided)
   └─ Same currency conversion logic
   ↓
3. Response includes:
   {
     "transferType": "external",
     "toAccount": {
       "userId": 456,
       "userName": "John Doe"
     }
   }
```

### Payment Approval Flow

```
1. PUT /api/payments/{id}/approve
   Authorization: Bearer <token> (CHECKER/ADMIN)
   ↓
2. PaymentController.approvePayment()
   ├─ Get current user
   ├─ Check role (RoleUtil.canApprovePayments())
   └─ If authorized → Call PaymentService
   ↓
3. PaymentService.approvePayment()
   ├─ Get payment from database
   ├─ Validate: status == PENDING
   ├─ Check balance: fromAccount.balance >= amount
   ├─ BEGIN TRANSACTION
   │  ├─ Deduct from fromAccount (in fromAccount currency)
   │  ├─ Add to toAccount (in toAccount currency, converted)
   │  └─ Update payment status = APPROVED
   └─ COMMIT TRANSACTION
   ↓
4. Atomic Transaction
   ├─ All operations succeed → Commit
   └─ Any operation fails → Rollback
   ↓
5. Response
   {
     "id": 1,
     "status": "APPROVED",
     "message": "Payment approved successfully"
   }
```

### Payment Rejection Flow

```
1. PUT /api/payments/{id}/reject
   Authorization: Bearer <token> (CHECKER/ADMIN)
   ↓
2. PaymentService.rejectPayment()
   ├─ Validate: status == PENDING
   ├─ Update status = REJECTED
   └─ NO funds transferred
   ↓
3. Response
   {
     "id": 1,
     "status": "REJECTED",
     "message": "Payment rejected"
   }
```

---

## Exchange Rate Flow

### Exchange Rate Calculation Flow

```
1. Payment Creation with Different Currencies
   fromAccount.currency = "USD"
   toAccount.currency = "CAD"
   ↓
2. PaymentService.createPayment()
   ├─ Detect currencies differ
   └─ Call ExchangeRateService.getExchangeRate()
   ↓
3. ExchangeRateService.getExchangeRate("USD", "CAD")
   ├─ Try direct rate: USD → CAD
   ├─ If not found, try reverse: CAD → USD (calculate 1/rate)
   ├─ If not found, try intermediate: USD → EUR → CAD
   └─ Return exchange rate
   ↓
4. ExchangeRateService.convertAmount()
   ├─ Get exchange rate
   ├─ Calculate: convertedAmount = amount × rate
   └─ Round to 2 decimal places
   ↓
5. Payment stored with:
   ├─ amount: 100.00 (USD)
   ├─ currency: "USD"
   ├─ convertedAmount: 135.00 (CAD)
   ├─ convertedCurrency: "CAD"
   └─ exchangeRate: 1.35
```

### Exchange Rate Lookup Priority

```
1. Direct Rate
   USD → CAD exists? → Use it

2. Reverse Rate
   CAD → USD exists? → Calculate: 1 / rate

3. Intermediate Conversion
   USD → EUR → CAD
   Calculate: (USD → EUR rate) × (EUR → CAD rate)
```

---

## Database Schema

### Entity Relationships

```
User (1) ──< (Many) Account
   │
   └── Role: USER, CHECKER, ADMIN

Account (1) ──< (Many) Payment (as fromAccount)
Account (1) ──< (Many) Payment (as toAccount)

Payment
   ├── fromAccount (ManyToOne → Account)
   ├── toAccount (ManyToOne → Account)
   ├── approvedBy (ManyToOne → User)
   └── transferType: INTERNAL, EXTERNAL

ExchangeRate
   └── Independent entity (no relationships)
```

### Key Tables

1. **users**
   - id, username, email, password, role
   - firstName, lastName, phoneNumber
   - isActive, createdAt, updatedAt

2. **accounts**
   - id, account_number, account_type
   - balance, currency, is_active
   - user_id (FK → users)
   - created_at, updated_at

3. **payments**
   - id, transaction_id, amount, currency
   - converted_amount, converted_currency, exchange_rate
   - status, transfer_type
   - from_account_id, to_account_id (FK → accounts)
   - approved_by (FK → users)
   - description, created_at, approved_at

4. **exchange_rates**
   - id, from_currency, to_currency, rate
   - is_active, created_at, updated_at

---

## Security Architecture

### Security Layers

```
1. CORS Protection
   ├─ Only allows requests from http://localhost:5173
   └─ Configured in CorsConfig

2. JWT Authentication
   ├─ Token-based authentication
   ├─ Stateless (no server-side sessions)
   └─ Token expires after 24 hours

3. Spring Security
   ├─ Protected endpoints require authentication
   ├─ Public endpoints: /api/health, /api/auth/login, /api/auth/register
   └─ All other endpoints require valid JWT token

4. Role-Based Access Control
   ├─ USER: Can create payments, view own accounts
   ├─ CHECKER: Can approve/reject payments
   └─ ADMIN: Full access (approve/reject all payments)

5. Password Security
   ├─ BCrypt hashing (one-way encryption)
   └─ Never store plaintext passwords
```

### Security Flow

```
1. Public Request
   GET /api/health
   └─ No authentication required

2. Authentication Request
   POST /api/auth/login
   ├─ Validate credentials
   ├─ Generate JWT token
   └─ Return token

3. Protected Request
   GET /api/accounts
   ├─ Extract JWT token from header
   ├─ Validate token
   ├─ Extract user ID
   └─ Allow if valid

4. Role-Protected Request
   PUT /api/payments/{id}/approve
   ├─ Extract JWT token
   ├─ Validate token
   ├─ Get user role
   ├─ Check if role == CHECKER or ADMIN
   └─ Allow if authorized
```

---

## Key Design Patterns

### 1. **Dependency Injection**
- Spring manages all dependencies
- Constructor injection (`@RequiredArgsConstructor` from Lombok)
- Loose coupling between components

### 2. **Repository Pattern**
- Abstracts database operations
- Spring Data JPA provides implementation
- Easy to test and mock

### 3. **Service Layer Pattern**
- Business logic separated from controllers
- Transaction management (`@Transactional`)
- Reusable business logic

### 4. **DTO Pattern (Partial)**
- Controllers format responses
- Maps entities to response objects
- Hides internal entity structure

### 5. **Filter Pattern**
- JWT authentication filter
- Intercepts all requests
- Pre-processing before controllers

---

## Error Handling

### Error Response Format

```json
{
  "message": "Error description",
  "code": "ERROR_CODE"
}
```

### Common Error Codes

- `ACCOUNT_NOT_FOUND` - Account doesn't exist
- `INVALID_TRANSFER_TYPE` - Transfer type mismatch
- `INSUFFICIENT_BALANCE` - Not enough funds
- `ACCOUNT_INACTIVE` - Account is not active
- `UNAUTHORIZED_ACCESS` - Permission denied
- `SEARCH_QUERY_TOO_SHORT` - Search query < 3 characters

---

## Transaction Management

### Atomic Operations

```
@Transactional
public Payment approvePayment() {
    // All operations succeed or all fail
    1. Deduct from account
    2. Add to account
    3. Update payment status
    
    // If any step fails → Rollback all changes
}
```

### Transaction Isolation

- Default: READ_COMMITTED
- Ensures data consistency
- Prevents race conditions

---

## Summary

The TransactIQ backend follows a **clean, layered architecture** with:

✅ **Clear separation of concerns** (Controller → Service → Repository)  
✅ **Security-first design** (JWT, RBAC, password hashing)  
✅ **Transaction management** (atomic operations)  
✅ **Currency conversion** (automatic exchange rate handling)  
✅ **Role-based access control** (USER, CHECKER, ADMIN)  
✅ **External transfer support** (search, create, track)  
✅ **Comprehensive error handling** (structured error responses)  

The architecture is **scalable**, **maintainable**, and follows **Spring Boot best practices**.

