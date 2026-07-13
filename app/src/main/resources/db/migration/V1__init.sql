-- ═══════════════════════════════════════════════════════════════════════════
-- V1__init.sql — Initial Database Schema Migration (Flyway)
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Flyway executes this file when the application starts and the database
-- has no prior migrations. The "V1" prefix denotes version 1; subsequent
-- migrations must be V2, V3, etc. (or V1_1__...).
--
-- After running, Flyway creates a "flyway_schema_history" table to track
-- which migrations have been applied. This ensures migrations are only
-- run once and in order.
-- ═══════════════════════════════════════════════════════════════════════════

-- Tasks table: stores all task records
CREATE TABLE IF NOT EXISTS tasks (
    id          BIGSERIAL PRIMARY KEY,                           -- Auto-incrementing primary key
    title       VARCHAR(100) NOT NULL,                           -- Task title (required)
    description VARCHAR(500),                                    -- Optional description
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',         -- PENDING | IN_PROGRESS | COMPLETED | CANCELLED
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',          -- LOW | MEDIUM | HIGH | CRITICAL
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),              -- Auto-set on insert
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()               -- Auto-set on update
);

-- Index on status (frequently filtered column)
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);

-- Index on priority (frequently filtered column)
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);

-- Index on created_at (for chronological sorting)
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at);

-- Updated_at trigger — auto-update on row modification
-- (Hibernate @UpdateTimestamp also does this, but this is a database-level guarantee)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();