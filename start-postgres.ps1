# PowerShell script to start PostgreSQL with Docker

Write-Host "Checking Docker Desktop..." -ForegroundColor Yellow

# Check if Docker is running
try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Docker Desktop is running!" -ForegroundColor Green
    } else {
        Write-Host "❌ Docker Desktop is not running!" -ForegroundColor Red
        Write-Host "Please start Docker Desktop and try again." -ForegroundColor Yellow
        Write-Host "Press any key to exit..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 1
    }
} catch {
    Write-Host "❌ Docker Desktop is not running!" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and try again." -ForegroundColor Yellow
    Write-Host "Press any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

Write-Host "`nStarting PostgreSQL container..." -ForegroundColor Yellow
docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ PostgreSQL is starting!" -ForegroundColor Green
    Write-Host "Waiting for PostgreSQL to be ready..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    # Check if container is running
    $containerStatus = docker ps --filter "name=transactiq-postgres" --format "{{.Status}}"
    if ($containerStatus) {
        Write-Host "✅ PostgreSQL container is running!" -ForegroundColor Green
        Write-Host "Container Status: $containerStatus" -ForegroundColor Cyan
        Write-Host "`nDatabase Details:" -ForegroundColor Yellow
        Write-Host "  Host: localhost" -ForegroundColor Cyan
        Write-Host "  Port: 5432" -ForegroundColor Cyan
        Write-Host "  Database: transactiq_db" -ForegroundColor Cyan
        Write-Host "  Username: postgres" -ForegroundColor Cyan
        Write-Host "  Password: postgres" -ForegroundColor Cyan
        Write-Host "`n✅ You can now start your Spring Boot application!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Container may still be starting. Check logs with: docker-compose logs postgres" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Failed to start PostgreSQL. Check Docker Desktop is running." -ForegroundColor Red
}

Write-Host "`nUseful commands:" -ForegroundColor Yellow
Write-Host "  View logs: docker-compose logs postgres" -ForegroundColor Cyan
Write-Host "  Stop: docker-compose down" -ForegroundColor Cyan
Write-Host "  Check status: docker ps" -ForegroundColor Cyan


