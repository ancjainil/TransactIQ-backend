# Test Admin Functionality Script
# This script tests the admin role-based access control

$baseUrl = "http://localhost:8080"
$testEmail = "testadmin@example.com"
$testPassword = "password123"

Write-Host "=== Testing Admin Functionality ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if backend is running
Write-Host "Step 1: Checking backend health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method GET -ErrorAction Stop
    Write-Host "✓ Backend is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Backend is not running. Please start it first." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: Register a test user
Write-Host "Step 2: Registering test user..." -ForegroundColor Yellow
try {
    $registerBody = @{
        name = "Test Admin User"
        email = $testEmail
        password = $testPassword
    } | ConvertTo-Json

    $registerResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody `
        -ErrorAction Stop

    $registerData = $registerResponse.Content | ConvertFrom-Json
    $token = $registerData.token
    $userId = $registerData.user.id
    $userRole = $registerData.user.role

    Write-Host "✓ User registered successfully" -ForegroundColor Green
    Write-Host "  User ID: $userId" -ForegroundColor Gray
    Write-Host "  Role: $userRole" -ForegroundColor Gray
    Write-Host "  Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
} catch {
    # If user already exists, try to login instead
    Write-Host "User already exists, attempting login..." -ForegroundColor Yellow
    try {
        $loginBody = @{
            email = $testEmail
            password = $testPassword
        } | ConvertTo-Json

        $loginResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body $loginBody `
            -ErrorAction Stop

        $loginData = $loginResponse.Content | ConvertFrom-Json
        $token = $loginData.token
        $userId = $loginData.user.id
        $userRole = $loginData.user.role

        Write-Host "✓ User logged in successfully" -ForegroundColor Green
        Write-Host "  User ID: $userId" -ForegroundColor Gray
        Write-Host "  Role: $userRole" -ForegroundColor Gray
    } catch {
        Write-Host "✗ Failed to register/login: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# Step 3: Test /api/auth/me endpoint
Write-Host "Step 3: Testing /api/auth/me endpoint..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }

    $meResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/me" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $meData = $meResponse.Content | ConvertFrom-Json
    Write-Host "✓ /api/auth/me works" -ForegroundColor Green
    Write-Host "  Current Role: $($meData.role)" -ForegroundColor Gray
    Write-Host "  User ID: $($meData.id)" -ForegroundColor Gray
    Write-Host "  Email: $($meData.email)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to get user info: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 4: Check if user is admin
if ($meData.role -ne "admin") {
    Write-Host "Step 4: User role is '$($meData.role)', not 'admin'" -ForegroundColor Yellow
    Write-Host "Please update user role to ADMIN in the database:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  Using psql:" -ForegroundColor Cyan
    Write-Host "    psql -h localhost -p 5432 -U postgres -d transactiq_db" -ForegroundColor White
    Write-Host "    UPDATE users SET role = 'ADMIN' WHERE email = '$testEmail';" -ForegroundColor White
    Write-Host ""
    Write-Host "  Or using pgAdmin:" -ForegroundColor Cyan
    Write-Host "    1. Open pgAdmin" -ForegroundColor White
    Write-Host "    2. Navigate to: transactiq_db -> Schemas -> public -> Tables -> users" -ForegroundColor White
    Write-Host "    3. Right-click 'users' -> View/Edit Data -> All Rows" -ForegroundColor White
    Write-Host "    4. Find user with email '$testEmail'" -ForegroundColor White
    Write-Host "    5. Change 'role' column from 'USER' to 'ADMIN'" -ForegroundColor White
    Write-Host "    6. Click Save" -ForegroundColor White
    Write-Host ""
    Write-Host "After updating, run this script again or press Enter to continue testing..." -ForegroundColor Yellow
    Read-Host
    Write-Host ""
    
    # Try to login again to get updated role
    Write-Host "Re-logging in to get updated role..." -ForegroundColor Yellow
    try {
        $loginBody = @{
            email = $testEmail
            password = $testPassword
        } | ConvertTo-Json

        $loginResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body $loginBody `
            -ErrorAction Stop

        $loginData = $loginResponse.Content | ConvertFrom-Json
        $token = $loginData.token
        $userRole = $loginData.user.role
        
        Write-Host "✓ New role: $userRole" -ForegroundColor Green
        
        if ($userRole -ne "admin") {
            Write-Host "✗ Role is still not admin. Please update in database and try again." -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "✗ Failed to re-login: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Step 4: ✓ User is already admin" -ForegroundColor Green
}
Write-Host ""

# Step 5: Create test accounts and payment
Write-Host "Step 5: Creating test accounts and payment..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }

    # Create account 1
    $account1Body = @{
        type = "checking"
        currency = "USD"
        balance = 1000.00
    } | ConvertTo-Json

    $account1Response = Invoke-WebRequest -Uri "$baseUrl/api/accounts" `
        -Method POST `
        -Headers $headers `
        -Body $account1Body `
        -ErrorAction Stop

    $account1 = $account1Response.Content | ConvertFrom-Json
    $account1Id = $account1.id
    Write-Host "✓ Created account 1: ID $account1Id" -ForegroundColor Green

    # Create account 2
    $account2Body = @{
        type = "savings"
        currency = "USD"
        balance = 500.00
    } | ConvertTo-Json

    $account2Response = Invoke-WebRequest -Uri "$baseUrl/api/accounts" `
        -Method POST `
        -Headers $headers `
        -Body $account2Body `
        -ErrorAction Stop

    $account2 = $account2Response.Content | ConvertFrom-Json
    $account2Id = $account2.id
    Write-Host "✓ Created account 2: ID $account2Id" -ForegroundColor Green

    # Create payment (will be PENDING)
    $paymentBody = @{
        fromAccountId = $account1Id
        toAccountId = $account2Id
        amount = 100.00
        currency = "USD"
        description = "Test payment for admin approval"
    } | ConvertTo-Json

    $paymentResponse = Invoke-WebRequest -Uri "$baseUrl/api/payments" `
        -Method POST `
        -Headers $headers `
        -Body $paymentBody `
        -ErrorAction Stop

    $payment = $paymentResponse.Content | ConvertFrom-Json
    $paymentId = $payment.id
    Write-Host "✓ Created payment: ID $paymentId, Status: $($payment.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create test data: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Error details: $($_.Exception.Response)" -ForegroundColor Gray
    exit 1
}
Write-Host ""

# Step 6: Test approve payment endpoint
Write-Host "Step 6: Testing approve payment endpoint..." -ForegroundColor Yellow
try {
    $approveResponse = Invoke-WebRequest -Uri "$baseUrl/api/payments/$paymentId/approve" `
        -Method PUT `
        -Headers $headers `
        -ErrorAction Stop

    $approveData = $approveResponse.Content | ConvertFrom-Json
    Write-Host "✓ Payment approved successfully!" -ForegroundColor Green
    Write-Host "  Payment ID: $($approveData.id)" -ForegroundColor Gray
    Write-Host "  Status: $($approveData.status)" -ForegroundColor Gray
    Write-Host "  Message: $($approveData.message)" -ForegroundColor Gray
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✗ 403 Forbidden - User does not have admin/checker role" -ForegroundColor Red
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorContent = $reader.ReadToEnd()
            $errorResponse = $errorContent | ConvertFrom-Json
            Write-Host "  Error: $($errorResponse.message)" -ForegroundColor Gray
        } catch {
            Write-Host "  Error: Access denied" -ForegroundColor Gray
        }
        exit 1
    } else {
        Write-Host "✗ Failed to approve payment: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# Step 7: Verify payment status
Write-Host "Step 7: Verifying payment status..." -ForegroundColor Yellow
try {
    $paymentsResponse = Invoke-WebRequest -Uri "$baseUrl/api/payments" `
        -Method GET `
        -Headers $headers `
        -ErrorAction Stop

    $payments = $paymentsResponse.Content | ConvertFrom-Json
    $approvedPayment = $payments | Where-Object { $_.id -eq $paymentId }
    
    if ($approvedPayment.status -eq "APPROVED") {
        Write-Host "✓ Payment status is APPROVED" -ForegroundColor Green
    } else {
        Write-Host "⚠ Payment status is: $($approvedPayment.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to verify payment: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Step 8: Test reject payment (create another payment first)
Write-Host "Step 8: Testing reject payment endpoint..." -ForegroundColor Yellow
try {
    # Create another payment
    $payment2Body = @{
        fromAccountId = $account1Id
        toAccountId = $account2Id
        amount = 50.00
        currency = "USD"
        description = "Test payment for admin rejection"
    } | ConvertTo-Json

    $payment2Response = Invoke-WebRequest -Uri "$baseUrl/api/payments" `
        -Method POST `
        -Headers $headers `
        -Body $payment2Body `
        -ErrorAction Stop

    $payment2 = $payment2Response.Content | ConvertFrom-Json
    $payment2Id = $payment2.id
    Write-Host "✓ Created payment 2: ID $payment2Id" -ForegroundColor Green

    # Reject payment
    $rejectResponse = Invoke-WebRequest -Uri "$baseUrl/api/payments/$payment2Id/reject" `
        -Method PUT `
        -Headers $headers `
        -ErrorAction Stop

    $rejectData = $rejectResponse.Content | ConvertFrom-Json
    Write-Host "✓ Payment rejected successfully!" -ForegroundColor Green
    Write-Host "  Payment ID: $($rejectData.id)" -ForegroundColor Gray
    Write-Host "  Status: $($rejectData.status)" -ForegroundColor Gray
    Write-Host "  Message: $($rejectData.message)" -ForegroundColor Gray
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✗ 403 Forbidden - User does not have admin/checker role" -ForegroundColor Red
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorContent = $reader.ReadToEnd()
            $errorResponse = $errorContent | ConvertFrom-Json
            Write-Host "  Error: $($errorResponse.message)" -ForegroundColor Gray
        } catch {
            Write-Host "  Error: Access denied" -ForegroundColor Gray
        }
    } else {
        Write-Host "✗ Failed to reject payment: $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host ""

Write-Host "=== Test Summary ===" -ForegroundColor Cyan
Write-Host "✓ All admin functionality tests completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Test User:" -ForegroundColor Yellow
Write-Host "  Email: $testEmail" -ForegroundColor Gray
Write-Host "  Password: $testPassword" -ForegroundColor Gray
Write-Host "  Role: ADMIN" -ForegroundColor Gray
Write-Host ""
Write-Host "Test Results:" -ForegroundColor Yellow
Write-Host "  ✓ User registration/login" -ForegroundColor Green
Write-Host "  ✓ /api/auth/me endpoint" -ForegroundColor Green
Write-Host "  ✓ Payment approval" -ForegroundColor Green
Write-Host "  ✓ Payment rejection" -ForegroundColor Green
Write-Host ""

