# PostgreSQL Setup Guide

## Option 1: Using Docker (Recommended - Easiest)

### Prerequisites
- Install Docker Desktop: https://www.docker.com/products/docker-desktop/

### Steps
1. **Start PostgreSQL with Docker:**
   ```bash
   docker-compose up -d
   ```

2. **Verify it's running:**
   ```bash
   docker ps
   ```
   You should see `transactiq-postgres` container running.

3. **Check logs:**
   ```bash
   docker-compose logs postgres
   ```

4. **Stop PostgreSQL:**
   ```bash
   docker-compose down
   ```

5. **Stop and remove data:**
   ```bash
   docker-compose down -v
   ```

## Option 2: Local PostgreSQL Installation

### Windows Installation

1. **Download PostgreSQL:**
   - Visit: https://www.postgresql.org/download/windows/
   - Download the installer from EnterpriseDB

2. **Install PostgreSQL:**
   - Run the installer
   - Set password: `postgres` (or update `application.yml` with your password)
   - Port: `5432` (default)
   - Complete the installation

3. **Create Database:**
   Open PostgreSQL Command Line (psql) or pgAdmin and run:
   ```sql
   CREATE DATABASE transactiq_db;
   ```

4. **Verify Connection:**
   ```bash
   psql -U postgres -d transactiq_db
   ```

### Configuration

The application is already configured in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/transactiq_db
    username: postgres
    password: postgres
```

If you use different credentials, update `application.yml` or set environment variables:
```bash
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

## Option 3: Using H2 In-Memory Database (For Quick Testing)

If you want to test without PostgreSQL, I can configure H2 instead.

## Testing the Connection

Once PostgreSQL is running:

1. **Start the backend:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

2. **You should see:**
   - `Tomcat started on port(s): 8080 (http)`
   - No database connection errors

3. **Test the health endpoint:**
   ```bash
   curl http://localhost:8080/api/health
   ```

## Troubleshooting

### Port 5432 already in use
- Check what's using it: `netstat -ano | findstr :5432`
- Change PostgreSQL port or update `application.yml`

### Connection refused
- Make sure PostgreSQL is running
- Check if it's listening on localhost:5432
- Verify firewall settings

### Authentication failed
- Check username/password in `application.yml`
- Verify PostgreSQL user permissions

## Quick Start Commands

```bash
# Start PostgreSQL (Docker)
docker-compose up -d

# Start backend
.\mvnw.cmd spring-boot:run

# Stop PostgreSQL (Docker)
docker-compose down
```


