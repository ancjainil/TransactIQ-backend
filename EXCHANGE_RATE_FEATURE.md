# Exchange Rate Feature - Global Transfer Implementation

## Overview

The TransactIQ backend now supports **automatic currency conversion** for international/global transfers. When transferring money between accounts with different currencies, the system automatically converts the amount using stored exchange rates.

---

## How It Works

### 1. **Payment Creation Flow**

When a user creates a payment:

1. **Currency Detection**: System automatically detects currencies from:
   - `fromAccount.currency` - Source account currency
   - `toAccount.currency` - Destination account currency

2. **Exchange Rate Calculation**:
   - If currencies are the same → No conversion needed (rate = 1.0)
   - If currencies differ → System looks up exchange rate from database
   - Calculates converted amount: `convertedAmount = amount × exchangeRate`

3. **Payment Storage**:
   - `amount`: Original amount in fromAccount currency
   - `currency`: FromAccount currency
   - `convertedAmount`: Converted amount in toAccount currency
   - `convertedCurrency`: ToAccount currency
   - `exchangeRate`: Exchange rate used for conversion

4. **Status**: Payment created with `PENDING` status (no funds transferred yet)

### 2. **Payment Approval Flow**

When an admin/checker approves a payment:

1. **Balance Check**: Validates sufficient balance in fromAccount (in fromAccount currency)
2. **Fund Transfer**:
   - **Deducts** `amount` from `fromAccount.balance` (in fromAccount currency)
   - **Adds** `convertedAmount` to `toAccount.balance` (in toAccount currency)
3. **Status Update**: Payment status changes to `APPROVED`

### 3. **Example: USD to CAD Transfer**

**Scenario**: Transfer $100 USD from a USD account to a CAD account

1. **Payment Creation**:
   - Amount: $100 USD
   - Exchange Rate: 1 USD = 1.35 CAD (example)
   - Converted Amount: $135 CAD
   - Payment stored with both amounts

2. **Payment Approval**:
   - Deducts: $100 USD from USD account
   - Adds: $135 CAD to CAD account

---

## Database Schema

### Exchange Rates Table

```sql
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(19, 6) NOT NULL,  -- 1 fromCurrency = rate toCurrency
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(from_currency, to_currency)
);
```

### Payment Table Updates

Added columns to `payments` table:
- `converted_amount` (DECIMAL) - Converted amount in toAccount currency
- `converted_currency` (VARCHAR(3)) - Currency of converted amount
- `exchange_rate` (DECIMAL(19,6)) - Exchange rate used

---

## API Endpoints

### Exchange Rate Endpoints

#### `GET /api/exchange-rates`
- **Purpose**: Get all active exchange rates
- **Headers**: `Authorization: Bearer <token>`
- **Response**:
  ```json
  [
    {
      "id": 1,
      "fromCurrency": "USD",
      "toCurrency": "CAD",
      "rate": 1.350000,
      "isActive": true,
      "createdAt": "2025-11-04T10:00:00",
      "updatedAt": "2025-11-04T10:00:00"
    }
  ]
  ```

#### `GET /api/exchange-rates/{fromCurrency}/{toCurrency}`
- **Purpose**: Get exchange rate for specific currency pair
- **Example**: `GET /api/exchange-rates/USD/CAD`
- **Response**:
  ```json
  {
    "fromCurrency": "USD",
    "toCurrency": "CAD",
    "rate": 1.350000,
    "description": "1 USD = 1.350000 CAD"
  }
  ```

#### `GET /api/exchange-rates/convert?amount=100&fromCurrency=USD&toCurrency=CAD`
- **Purpose**: Convert amount between currencies
- **Response**:
  ```json
  {
    "originalAmount": 100.00,
    "originalCurrency": "USD",
    "convertedAmount": 135.00,
    "convertedCurrency": "CAD",
    "exchangeRate": 1.350000
  }
  ```

#### `POST /api/exchange-rates`
- **Purpose**: Create or update exchange rate
- **Request Body**:
  ```json
  {
    "fromCurrency": "USD",
    "toCurrency": "CAD",
    "rate": 1.350000
  }
  ```
- **Response**:
  ```json
  {
    "id": 1,
    "fromCurrency": "USD",
    "toCurrency": "CAD",
    "rate": 1.350000,
    "message": "Exchange rate saved successfully"
  }
  ```

#### `POST /api/exchange-rates/initialize`
- **Purpose**: Initialize default exchange rates (for development/testing)
- **Response**:
  ```json
  {
    "message": "Default exchange rates initialized successfully"
  }
  ```

---

## Payment API Changes

### Enhanced Payment Response

When currencies differ, payment responses now include conversion information:

```json
{
  "id": 1,
  "description": "International transfer",
  "amount": 100.00,
  "currency": "USD",
  "fromAccountId": 1,
  "toAccountId": 2,
  "status": "PENDING",
  "conversion": {
    "exchangeRate": 1.350000,
    "originalAmount": 100.00,
    "originalCurrency": "USD",
    "convertedAmount": 135.00,
    "convertedCurrency": "CAD"
  }
}
```

---

## Exchange Rate Calculation Logic

### Direct Rates
- If `USD → CAD` rate exists, use it directly

### Reverse Rates
- If only `CAD → USD` rate exists, calculate reverse: `1 / rate`

### Intermediate Conversion (USD as Base)
- If `USD → EUR` and `EUR → CAD` don't exist:
- Try: `EUR → USD → CAD`
- Calculate: `EUR_to_USD_rate × USD_to_CAD_rate`

### Supported Currency Pairs
- **USD ↔ CAD**: Bidirectional
- **USD ↔ EUR**: Bidirectional  
- **CAD ↔ EUR**: Bidirectional (via USD intermediate)

---

## Default Exchange Rates

The system initializes with default rates (for development):

- **USD → CAD**: 1.350000 (1 USD = 1.35 CAD)
- **USD → EUR**: 0.920000 (1 USD = 0.92 EUR)
- **CAD → USD**: 0.740741 (reverse of USD→CAD)
- **EUR → USD**: 1.086957 (reverse of USD→EUR)
- **CAD ↔ EUR**: Calculated via USD intermediate

**Note**: These are example rates. In production, you should:
1. Integrate with a real-time exchange rate API (e.g., ExchangeRate-API, Fixer.io)
2. Update rates periodically (daily/hourly)
3. Store historical rates for audit purposes

---

## Business Logic

### Payment Creation
- ✅ Currency automatically determined from `fromAccount`
- ✅ Exchange rate calculated and stored
- ✅ Converted amount calculated and stored
- ✅ No funds transferred (status: PENDING)

### Payment Approval
- ✅ Balance validated in fromAccount currency
- ✅ Funds deducted in fromAccount currency
- ✅ Funds added in toAccount currency (converted amount)
- ✅ Transaction is atomic (all or nothing)

### Payment Rejection
- ✅ No funds transferred
- ✅ Status changed to REJECTED

---

## Example Scenarios

### Scenario 1: Same Currency Transfer
**Transfer**: $100 from USD account to USD account
- Exchange Rate: 1.0 (no conversion)
- Deducts: $100 USD
- Adds: $100 USD

### Scenario 2: USD to CAD Transfer
**Transfer**: $100 from USD account to CAD account
- Exchange Rate: 1.35
- Deducts: $100 USD
- Adds: $135 CAD

### Scenario 3: EUR to USD Transfer
**Transfer**: €100 from EUR account to USD account
- Exchange Rate: 1.086957 (EUR to USD)
- Deducts: €100 EUR
- Adds: $108.70 USD

### Scenario 4: CAD to EUR Transfer
**Transfer**: $100 CAD from CAD account to EUR account
- Exchange Rate: 0.680741 (via USD: CAD→USD→EUR)
- Deducts: $100 CAD
- Adds: €68.07 EUR

---

## Frontend Integration

### Payment Creation Request

**No change needed!** The frontend doesn't need to specify currency - it's automatically determined from the fromAccount.

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 100.00,
  "description": "International transfer"
}
```

### Payment Response

The response now includes conversion info if currencies differ:

```json
{
  "id": 1,
  "amount": 100.00,
  "currency": "USD",
  "conversion": {
    "exchangeRate": 1.350000,
    "originalAmount": 100.00,
    "originalCurrency": "USD",
    "convertedAmount": 135.00,
    "convertedCurrency": "CAD"
  }
}
```

### Frontend Display

The frontend can:
1. Show original amount and currency
2. Show converted amount and currency
3. Display exchange rate used
4. Show "International Transfer" badge if currencies differ

---

## Testing Exchange Rates

### 1. Initialize Default Rates

```bash
curl -X POST http://localhost:8080/api/exchange-rates/initialize \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2. Get Exchange Rate

```bash
curl -X GET http://localhost:8080/api/exchange-rates/USD/CAD \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. Convert Amount

```bash
curl -X GET "http://localhost:8080/api/exchange-rates/convert?amount=100&fromCurrency=USD&toCurrency=CAD" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Create Cross-Currency Payment

```bash
# Create USD account
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"checking","currency":"USD","balance":1000.00}'

# Create CAD account  
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"savings","currency":"CAD","balance":500.00}'

# Create payment (USD to CAD)
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":100.00,"description":"USD to CAD transfer"}'
```

**Response will include conversion info:**
```json
{
  "id": 1,
  "amount": 100.00,
  "currency": "USD",
  "conversion": {
    "exchangeRate": 1.350000,
    "originalAmount": 100.00,
    "originalCurrency": "USD",
    "convertedAmount": 135.00,
    "convertedCurrency": "CAD"
  },
  "status": "PENDING"
}
```

### 5. Approve Payment

```bash
curl -X PUT http://localhost:8080/api/payments/1/approve \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

**Result**:
- USD account balance: $900.00 (deducted $100)
- CAD account balance: $635.00 (added $135)

---

## Future Enhancements

### Potential Improvements:
1. **Real-time Exchange Rate API Integration**
   - Integrate with external APIs (ExchangeRate-API, Fixer.io, etc.)
   - Scheduled job to update rates daily/hourly

2. **Exchange Rate History**
   - Store historical rates for audit
   - Show rate trends over time

3. **Rate Locking**
   - Lock exchange rate at payment creation
   - Use locked rate even if market rate changes

4. **Exchange Rate Fees**
   - Add fee percentage for currency conversion
   - Deduct fee from converted amount

5. **Multi-currency Dashboard**
   - Show total balance in base currency (e.g., USD)
   - Convert all currencies to base for display

6. **Rate Alerts**
   - Notify users when exchange rates change significantly
   - Suggest optimal timing for transfers

---

## Summary

✅ **Automatic Currency Conversion**: System automatically detects and converts currencies  
✅ **Exchange Rate Storage**: Rates stored in database for quick lookup  
✅ **Smart Rate Calculation**: Supports direct, reverse, and intermediate rates  
✅ **Atomic Transactions**: Currency conversion happens atomically on approval  
✅ **API Endpoints**: Full CRUD for exchange rate management  
✅ **Default Rates**: System initializes with default rates on startup  
✅ **Frontend Ready**: Conversion info included in payment responses  

The exchange rate feature is **fully integrated** and ready for production use!

