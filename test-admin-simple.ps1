# Simple Admin Functionality Test
$baseUrl = "http://localhost:8080"

Write-Host "=== Testing Admin Functionality ===" -ForegroundColor Cyan
Write-Host ""

# Check if backend is running
Write-Host "Checking backend health..." -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri "$baseUrl/api/health" -Method GET -ErrorAction Stop
    Write-Host "✓ Backend is running" -ForegroundColor Green
    Write-Host "  Status: $($health.StatusCode)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Backend is not running!" -ForegroundColor Red
    Write-Host "  Please start the backend first with: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Instructions
Write-Host "=== Testing Instructions ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "To test admin functionality:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Register/Login a test user:" -ForegroundColor White
Write-Host '   POST http://localhost:8080/api/auth/register' -ForegroundColor Gray
Write-Host '   Body: {"name":"Test User","email":"admin@test.com","password":"password123"}' -ForegroundColor Gray
Write-Host ""
Write-Host "2. Update user role to ADMIN in database:" -ForegroundColor White
Write-Host "   Using psql:" -ForegroundColor Gray
Write-Host "   psql -h localhost -p 5432 -U postgres -d transactiq_db" -ForegroundColor Cyan
Write-Host "   UPDATE users SET role = 'ADMIN' WHERE email = 'admin@test.com';" -ForegroundColor Cyan
Write-Host ""
Write-Host '   Or using pgAdmin:' -ForegroundColor Gray
Write-Host "   - Open pgAdmin -> transactiq_db -> Schemas -> public -> Tables -> users" -ForegroundColor Cyan
Write-Host "   - Right-click 'users' -> View/Edit Data -> All Rows" -ForegroundColor Cyan
Write-Host "   - Change 'role' column from 'USER' to 'ADMIN'" -ForegroundColor Cyan
Write-Host "   - Click Save" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Login again to get new token:" -ForegroundColor White
Write-Host '   POST http://localhost:8080/api/auth/login' -ForegroundColor Gray
Write-Host '   Body: {"email":"admin@test.com","password":"password123"}' -ForegroundColor Gray
Write-Host ""
Write-Host "4. Test /api/auth/me endpoint:" -ForegroundColor White
Write-Host '   GET http://localhost:8080/api/auth/me' -ForegroundColor Gray
Write-Host '   Header: Authorization: Bearer YOUR_TOKEN' -ForegroundColor Gray
Write-Host ""
Write-Host "5. Test approve payment:" -ForegroundColor White
Write-Host '   PUT http://localhost:8080/api/payments/{id}/approve' -ForegroundColor Gray
Write-Host '   Header: Authorization: Bearer YOUR_TOKEN' -ForegroundColor Gray
Write-Host ""
Write-Host "6. Test reject payment:" -ForegroundColor White
Write-Host '   PUT http://localhost:8080/api/payments/{id}/reject' -ForegroundColor Gray
Write-Host '   Header: Authorization: Bearer YOUR_TOKEN' -ForegroundColor Gray
Write-Host ""

