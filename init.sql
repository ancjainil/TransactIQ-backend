-- Initialize TransactIQ Database
-- This script runs automatically when the PostgreSQL container starts

-- Create database if it doesn't exist (usually already created by POSTGRES_DB)
-- SELECT 'CREATE DATABASE transactiq_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'transactiq_db')\gexec

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE transactiq_db TO postgres;

-- You can add any initial data or schema here
-- Hibernate will auto-create tables based on your entities


