# Checker Workflow with Risk Scoring - Implementation Guide

## Overview
This document describes the checker workflow implementation with risk scoring and auto-approval for low-risk payments, similar to how banks handle payment approvals.

---

## Features Implemented

### 1. **Risk Scoring System**
- Automatic risk calculation for all payments (0-100 scale)
- Risk factors considered:
  - **Amount Risk** (0-30 points): Higher amounts = higher risk
  - **Currency Risk** (0-20 points): International transfers = higher risk
  - **Transfer Type Risk** (0-15 points): External transfers = higher risk
  - **Time Risk** (0-10 points): Unusual hours (2 AM - 6 AM) = higher risk
  - **Balance Risk** (0-15 points): Low remaining balance = higher risk
  - **History Risk** (0-10 points): First-time recipients = higher risk (placeholder)

### 2. **Auto-Approval for Low-Risk Payments**
- **Auto-approve conditions:**
  - Risk score ≤ 20 AND amount ≤ $10,000
  - OR Risk score ≤ 30 AND amount ≤ $1,000
- **Benefits:**
  - Small payments ($10, $50, $100) are auto-approved
  - No manual approval needed for low-risk transactions
  - Checkers focus on high-risk payments only

### 3. **Risk Levels**
- **LOW** (0-30): Auto-approved if amount is small
- **MEDIUM** (31-60): Requires manual approval
- **HIGH** (61-80): Requires manual approval, priority review
- **VERY_HIGH** (81-100): Requires manual approval, urgent review

### 4. **Separated CHECKER and ADMIN Roles**
- **CHECKER**: Can approve/reject payments, view approval queue, see risk statistics
- **ADMIN**: Can do everything checker can + manage users, view all accounts, system administration
- Clear separation of concerns

### 5. **Checker Dashboard**
- Approval queue sorted by risk score (highest first)
- Risk statistics (counts by level, average score)
- Payment details with risk information
- User information for external transfers

### 6. **Admin Dashboard**
- User management (view all users, update roles)
- System-wide statistics
- Access to all checker features

---

## Database Schema Changes

### Payments Table - New Columns
```sql
ALTER TABLE payments 
ADD COLUMN risk_score DECIMAL(5,2),
ADD COLUMN risk_level VARCHAR(20),
ADD COLUMN auto_approved BOOLEAN DEFAULT false;
```

**Risk Level Values:**
- `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`

---

## API Endpoints

### For CHECKER Role

#### `GET /api/checker/approval-queue`
**Purpose**: Get pending payments sorted by risk score

**Response:**
```json
{
  "pendingPayments": [
    {
      "id": 1,
      "transactionId": "TXN123",
      "amount": 5000.00,
      "currency": "USD",
      "riskScore": 45.50,
      "riskLevel": "MEDIUM",
      "transferType": "EXTERNAL",
      "fromAccount": {
        "id": 1,
        "accountNumber": "****1234",
        "balance": 10000.00,
        "userName": "John Doe",
        "userEmail": "john@example.com"
      },
      "toAccount": { ... },
      "conversion": { ... }
    }
  ],
  "totalPending": 10,
  "highRiskCount": 2,
  "mediumRiskCount": 5,
  "lowRiskCount": 3
}
```

#### `GET /api/checker/risk-statistics`
**Purpose**: Get risk statistics for dashboard

**Response:**
```json
{
  "lowRisk": 3,
  "mediumRisk": 5,
  "highRisk": 2,
  "veryHighRisk": 0,
  "averageRiskScore": 42.50,
  "totalPendingAmount": 50000.00,
  "autoApprovedCount": 15
}
```

#### `GET /api/checker/dashboard`
**Purpose**: Get complete checker dashboard (combines queue + statistics)

**Response:**
```json
{
  "approvalQueue": { ... },
  "riskStatistics": { ... },
  "userRole": "checker"
}
```

### For ADMIN Role

#### `GET /api/admin/users`
**Purpose**: Get all users (ADMIN only)

**Response:**
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "username": "user",
    "firstName": "John",
    "lastName": "Doe",
    "role": "user",
    "isActive": true,
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

#### `PUT /api/admin/users/{userId}/role`
**Purpose**: Update user role (ADMIN only)

**Request:**
```json
{
  "role": "checker"
}
```

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "role": "checker",
  "message": "User role updated successfully"
}
```

#### `GET /api/admin/dashboard`
**Purpose**: Get admin dashboard (ADMIN only)

**Response:**
```json
{
  "totalUsers": 50,
  "activeUsers": 45,
  "userCount": 40,
  "checkerCount": 5,
  "adminCount": 5
}
```

### Enhanced Payment Endpoints

#### `POST /api/payments` (Enhanced)
**Response now includes:**
```json
{
  "id": 1,
  "amount": 100.00,
  "currency": "USD",
  "riskScore": 5.00,
  "riskLevel": "LOW",
  "autoApproved": true,
  "status": "APPROVED"
}
```

#### `GET /api/payments` (Enhanced)
**Response now includes risk information:**
```json
[
  {
    "id": 1,
    "amount": 100.00,
    "riskScore": 5.00,
    "riskLevel": "LOW",
    "autoApproved": true,
    "status": "APPROVED"
  }
]
```

---

## Auto-Approval Logic

### When is a payment auto-approved?

1. **Very Low Risk (≤ 20)**: Auto-approve up to $10,000
   - Example: $5,000 internal transfer, same currency, normal hours
   - Risk Score: 0-20 → Auto-approved ✅

2. **Low Risk (21-30) AND Small Amount (≤ $1,000)**: Auto-approve
   - Example: $500 internal transfer, same currency
   - Risk Score: 25 → Auto-approved ✅

3. **Medium Risk (31-60)**: Requires manual approval
   - Example: $10,000 external transfer
   - Risk Score: 45 → PENDING, needs checker approval ⚠️

4. **High Risk (61-100)**: Requires manual approval
   - Example: $50,000 international transfer at 3 AM
   - Risk Score: 75 → PENDING, needs checker approval ⚠️

### Auto-Approval Examples

**Example 1: $10 Internal Transfer**
- Amount: $10
- Currency: Same (USD → USD)
- Transfer Type: Internal
- Time: Normal hours
- **Risk Score: 0**
- **Result: Auto-approved ✅**

**Example 2: $500 Internal Transfer**
- Amount: $500
- Currency: Same (USD → USD)
- Transfer Type: Internal
- Time: Normal hours
- **Risk Score: 0**
- **Result: Auto-approved ✅**

**Example 3: $1,500 Internal Transfer**
- Amount: $1,500
- Currency: Same (USD → USD)
- Transfer Type: Internal
- Time: Normal hours
- **Risk Score: 5** (amount > $1K)
- **Result: Auto-approved ✅** (risk ≤ 30 AND amount ≤ $1K? No, but risk ≤ 20? No, so...)
- **Wait, let me check the logic:**
  - Risk Score: 5 (≤ 20)
  - Amount: $1,500 (> $1,000)
  - **Result: Auto-approved ✅** (risk ≤ 20, so auto-approve up to $10K)

**Example 4: $5,000 External Transfer**
- Amount: $5,000
- Currency: Same (USD → USD)
- Transfer Type: External
- Time: Normal hours
- **Risk Score: 20** (15 for external + 5 for amount)
- **Result: Auto-approved ✅** (risk ≤ 20, amount ≤ $10K)

**Example 5: $10,000 International Transfer**
- Amount: $10,000
- Currency: Different (USD → CAD)
- Transfer Type: External
- Time: Normal hours
- **Risk Score: 40** (5 for amount + 20 for currency + 15 for external)
- **Result: PENDING** (requires manual approval)

---

## Role-Based Access

### USER Role
- ✅ Create payments
- ✅ View own accounts
- ✅ View own payments
- ✅ View simple dashboard (no risk scores)
- ❌ Cannot approve/reject payments
- ❌ Cannot see approval queue
- ❌ Cannot see risk statistics

### CHECKER Role
- ✅ All USER permissions
- ✅ View approval queue
- ✅ View risk statistics
- ✅ Approve/reject payments
- ✅ See risk scores for all payments
- ❌ Cannot manage users
- ❌ Cannot view all accounts

### ADMIN Role
- ✅ All CHECKER permissions
- ✅ Manage users (view, update roles)
- ✅ View all accounts
- ✅ Access admin dashboard
- ✅ System administration

---

## Frontend Integration

### Role-Based UI Components

#### USER Dashboard
```typescript
// Simple dashboard - no risk scores
GET /api/dashboard
// Shows: totalBalance, accounts, payments (own only)
```

#### CHECKER Dashboard
```typescript
// Checker dashboard with risk scores
GET /api/checker/dashboard
// Shows: approval queue, risk statistics, pending payments

// Approval queue
GET /api/checker/approval-queue
// Shows: pending payments sorted by risk (highest first)

// Risk statistics
GET /api/checker/risk-statistics
// Shows: risk distribution, average score
```

#### ADMIN Dashboard
```typescript
// Admin dashboard
GET /api/admin/dashboard
// Shows: user statistics, system overview

// User management
GET /api/admin/users
PUT /api/admin/users/{id}/role
```

### UI Components Needed

1. **Risk Score Badge**
   - Color-coded by risk level
   - LOW: Green
   - MEDIUM: Yellow
   - HIGH: Orange
   - VERY_HIGH: Red

2. **Approval Queue Table**
   - Sortable by risk score, amount, date
   - Filter by risk level
   - Show risk score prominently

3. **Risk Statistics Cards**
   - Total pending
   - High risk count
   - Average risk score
   - Auto-approved count

4. **Payment Card (Enhanced)**
   - Show risk score badge
   - Show auto-approved indicator
   - Show risk level

---

## Testing

### Test Auto-Approval

1. **Create $10 payment** (internal, same currency)
   - Expected: Auto-approved, status = APPROVED
   - Risk Score: 0

2. **Create $500 payment** (internal, same currency)
   - Expected: Auto-approved, status = APPROVED
   - Risk Score: 0

3. **Create $1,500 payment** (internal, same currency)
   - Expected: Auto-approved, status = APPROVED
   - Risk Score: 5

4. **Create $10,000 payment** (external, different currency)
   - Expected: PENDING, requires manual approval
   - Risk Score: 40+

### Test Checker Dashboard

1. **Login as CHECKER**
2. **GET /api/checker/dashboard**
   - Should see approval queue
   - Should see risk statistics
   - Should see pending payments sorted by risk

3. **Approve a payment**
   - PUT /api/payments/{id}/approve
   - Should work for CHECKER role

### Test Admin Features

1. **Login as ADMIN**
2. **GET /api/admin/users**
   - Should see all users

3. **PUT /api/admin/users/{id}/role**
   - Should be able to update user roles

---

## Summary

✅ **Risk Scoring**: Automatic calculation for all payments  
✅ **Auto-Approval**: Low-risk payments auto-approved (no manual intervention)  
✅ **Role Separation**: CHECKER vs ADMIN clearly separated  
✅ **Checker Dashboard**: Approval queue with risk scores  
✅ **Admin Dashboard**: User management and system stats  
✅ **Enhanced Payments**: Risk information in all payment responses  

The system now works like a bank:
- Small, low-risk payments are auto-approved
- Checkers focus on high-risk payments
- Clear role separation (checker vs admin)
- Risk-based prioritization

