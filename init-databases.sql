-- =====================================================
-- HomeGenie Database Initialization Script
-- Creates required databases for microservices
-- =====================================================

-- Create User Service Database
CREATE DATABASE homegenie_users;

-- Create Maintenance Service Database
CREATE DATABASE homegenie_maintenance;

-- Create Payment Service Database
CREATE DATABASE homegenie_payments;

-- Create Notification Service Database
CREATE DATABASE homegenie_notifications;

-- Grant privileges (if needed)
GRANT ALL PRIVILEGES ON DATABASE homegenie_users TO postgres;
GRANT ALL PRIVILEGES ON DATABASE homegenie_maintenance TO postgres;
GRANT ALL PRIVILEGES ON DATABASE homegenie_payments TO postgres;
GRANT ALL PRIVILEGES ON DATABASE homegenie_notifications TO postgres;

-- Connect to each database and create extensions if needed
\c homegenie_users;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c homegenie_maintenance;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c homegenie_payments;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c homegenie_notifications;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Return to default database
\c postgres;

-- Verification
SELECT datname FROM pg_database WHERE datname LIKE 'homegenie%';
