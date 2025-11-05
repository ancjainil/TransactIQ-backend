# Quick Start Guide

## Step 1: Start PostgreSQL

### Option A: Using Docker (Recommended)

1. **Start Docker Desktop** (if not already running)
   - Look for Docker Desktop in your system tray
   - Or open Docker Desktop application

2. **Start PostgreSQL:**
   ```powershell
   .\start-postgres.ps1
   ```
   
   Or manually:
   ```powershell
   docker-compose up -d
   ```

3. **Verify it's running:**
   ```powershell
   docker ps
   ```
   You should see `transactiq-postgres` container.

### Option B: Install PostgreSQL Locally

See `POSTGRES_SETUP.md` for detailed instructions.

## Step 2: Start the Backend

Once PostgreSQL is running:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait for:
```
Tomcat started on port(s): 8080 (http)
```

## Step 3: Test the Backend

### Test Health Endpoint:
```powershell
curl http://localhost:8080/api/health
```

Or open in browser: `http://localhost:8080/api/health`

### Test from React Frontend:
```javascript
fetch('http://localhost:8080/api/health')
  .then(res => res.json())
  .then(data => console.log(data));
```

## Troubleshooting

### Docker Desktop not running
- Start Docker Desktop application
- Wait for it to fully start (whale icon in system tray)
- Then run: `.\start-postgres.ps1`

### Port 5432 already in use
- Another PostgreSQL instance might be running
- Check: `netstat -ano | findstr :5432`
- Stop the other instance or change port in `docker-compose.yml`

### Backend won't start
- Make sure PostgreSQL is running first
- Check logs: `docker-compose logs postgres`
- Verify database connection in `application.yml`

## Stop Everything

```powershell
# Stop backend (Ctrl+C in terminal)

# Stop PostgreSQL
docker-compose down

# Stop and remove all data
docker-compose down -v
```


