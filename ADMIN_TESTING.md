# Admin Functionality Testing Guide

## Prerequisites

1. **Backend must be running:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

2. **Database must be running:**
   ```bash
   docker ps
   # Should show transactiq-postgres container
   ```

## Testing Steps

### Step 1: Register/Login a Test User

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Test Admin\",\"email\":\"admin@test.com\",\"password\":\"password123\"}"

# Or Login (if already registered)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@test.com\",\"password\":\"password123\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGci...",
  "user": {
    "id": 1,
    "email": "admin@test.com",
    "name": "Test Admin",
    "username": "admin",
    "role": "user"
  }
}
```

**Note:** Role will be "user" initially.

---

### Step 2: Update User Role to ADMIN in Database

**Option A: Using psql**
```bash
psql -h localhost -p 5432 -U postgres -d transactiq_db

UPDATE users SET role = 'ADMIN' WHERE email = 'admin@test.com';
SELECT id, email, username, role FROM users WHERE email = 'admin@test.com';
\q
```

**Option B: Using pgAdmin**
1. Open pgAdmin
2. Connect to your database
3. Navigate to: `transactiq_db` → `Schemas` → `public` → `Tables` → `users`
4. Right-click `users` → "View/Edit Data" → "All Rows"
5. Find user with email `admin@test.com`
6. Change `role` column from `USER` to `ADMIN`
7. Click "Save"

---

### Step 3: Login Again to Get New Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@test.com\",\"password\":\"password123\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGci...",
  "user": {
    "id": 1,
    "email": "admin@test.com",
    "name": "Test Admin",
    "username": "admin",
    "role": "admin"  ← Should be "admin" now
  }
}
```

**Save the token** for next steps.

---

### Step 4: Test `/api/auth/me` Endpoint

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Expected Response:**
```json
{
  "id": 1,
  "email": "admin@test.com",
  "name": "Test Admin",
  "username": "admin",
  "role": "admin"
}
```

---

### Step 5: Create Test Accounts and Payment

```bash
# Create Account 1
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"checking\",\"currency\":\"USD\",\"balance\":1000.00}"

# Create Account 2
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"savings\",\"currency\":\"USD\",\"balance\":500.00}"

# Create Payment (status will be PENDING)
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":100.00,\"currency\":\"USD\",\"description\":\"Test payment\"}"
```

**Note the payment ID** from the response (e.g., `"id": 1`)

---

### Step 6: Test Approve Payment Endpoint

```bash
curl -X PUT http://localhost:8080/api/payments/1/approve \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Success Response (200 OK):**
```json
{
  "id": 1,
  "status": "APPROVED",
  "message": "Payment approved successfully"
}
```

**Expected Error Response (403 Forbidden) if not admin:**
```json
{
  "message": "Only checkers and admins can approve payments",
  "error": "Forbidden",
  "code": "FORBIDDEN"
}
```

---

### Step 7: Test Reject Payment Endpoint

```bash
# First create another payment
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":50.00,\"currency\":\"USD\",\"description\":\"Test payment 2\"}"

# Then reject it
curl -X PUT http://localhost:8080/api/payments/2/reject \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Success Response (200 OK):**
```json
{
  "id": 2,
  "status": "REJECTED",
  "message": "Payment rejected"
}
```

---

### Step 8: Verify Payment Status

```bash
curl -X GET http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected:** Payments should show updated statuses (APPROVED, REJECTED)

---

## Testing Checklist

- [ ] Backend is running
- [ ] Database is running
- [ ] Test user registered/logged in
- [ ] User role updated to ADMIN in database
- [ ] User logged in again (role should be "admin")
- [ ] `/api/auth/me` returns role "admin"
- [ ] Test accounts created
- [ ] Test payment created (status: PENDING)
- [ ] Approve payment endpoint works (200 OK)
- [ ] Reject payment endpoint works (200 OK)
- [ ] Payment statuses updated correctly

---

## Troubleshooting

### 403 Forbidden Error
- **Cause:** User role is not ADMIN or CHECKER in database
- **Solution:** Update role in database and login again

### 401 Unauthorized Error
- **Cause:** Missing or invalid JWT token
- **Solution:** Login again to get a fresh token

### Payment Not Found
- **Cause:** Payment ID doesn't exist
- **Solution:** Check payment ID from GET /api/payments response

### Backend Not Running
- **Solution:** Start backend with `.\mvnw.cmd spring-boot:run`

### Database Not Running
- **Solution:** Start database with `docker-compose up -d` or `.\start-postgres.ps1`

---

## Notes

- Role is stored in database, not in JWT token
- You must login again after updating role in database
- JWT token doesn't contain role - backend looks it up from database
- Only CHECKER and ADMIN roles can approve/reject payments
- Default role for new registrations is "user"

