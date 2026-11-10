-- ==========================================================
-- BalanceVault - PostgreSQL Database Roles & Security Matrix
-- ==========================================================

-- 1. Create Roles with principle of least privilege
DO $$
BEGIN
    -- Role: Admin / DDL Migration Owner
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'balancevault_admin') THEN
        CREATE ROLE balancevault_admin WITH LOGIN PASSWORD 'AdminVaultPass2026!';
    END IF;

    -- Role: Application Runtime (DML Read/Write only)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'balancevault_app') THEN
        CREATE ROLE balancevault_app WITH LOGIN PASSWORD 'AppRuntimePass2026!';
    END IF;

    -- Role: Auditor (Strict Read-Only access to financial logs)
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'balancevault_audit') THEN
        CREATE ROLE balancevault_audit WITH LOGIN PASSWORD 'AuditorReadOnly2026!';
    END IF;
END
$$;

-- 2. Schema Grants
GRANT CONNECT ON DATABASE ledgerline_db TO balancevault_admin, balancevault_app, balancevault_audit;
GRANT USAGE ON SCHEMA public TO balancevault_admin, balancevault_app, balancevault_audit;

-- 3. Application Role Permissions (SELECT, INSERT, UPDATE, DELETE on tables; USAGE on sequences)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO balancevault_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO balancevault_app;

-- 4. Auditor Role Permissions (Strictly SELECT only - NO UPDATE, NO DELETE, NO INSERT)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO balancevault_audit;

-- 5. Revoke destructive privileges from runtime application
REVOKE DROP, TRUNCATE ON ALL TABLES IN SCHEMA public FROM balancevault_app;
REVOKE DROP, TRUNCATE, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public FROM balancevault_audit;
