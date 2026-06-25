-- ============================================
-- Migration: Add Idempotency Key to Form Submissions
-- Database: PostgreSQL (NOT SQLite)
-- Purpose: Prevent duplicate submissions from retried requests
-- ============================================

-- Step 1: Add idempotency_key column (nullable initially)
-- This allows existing submissions without a key
ALTER TABLE form_submissions 
ADD COLUMN idempotency_key VARCHAR(255) 
UNIQUE 
NULL;

-- Step 2: Add comment explaining the column purpose
COMMENT ON COLUMN form_submissions.idempotency_key IS 
'UUID for deduplication on retries. Prevents duplicate submissions when API call is retried after network timeout but before response is received.';

-- Step 3: Create unique index for idempotency_key
-- WHERE clause excludes NULL values (PostgreSQL allows multiple NULLs in UNIQUE constraints)
CREATE UNIQUE INDEX idx_form_submissions_idempotency_key 
ON form_submissions(idempotency_key) 
WHERE idempotency_key IS NOT NULL;

-- Step 4: Create composite index for efficient queries
-- Used when checking for duplicate + form_id
CREATE INDEX idx_form_submissions_form_and_idempotency 
ON form_submissions(form_id, idempotency_key) 
WHERE idempotency_key IS NOT NULL;

-- Step 5: Generate idempotency keys for existing submissions
-- Format: "legacy_<submission_id>_<timestamp>"
-- This ensures all submissions get a key (for backward compatibility)
UPDATE form_submissions 
SET idempotency_key = 'legacy_' || id || '_' || EXTRACT(EPOCH FROM created_at)::BIGINT
WHERE idempotency_key IS NULL;

-- Step 6: Verify migration
-- Expected: All rows have idempotency_key, no duplicates
SELECT COUNT(*) as total, 
       COUNT(DISTINCT idempotency_key) as unique_keys,
       COUNT(CASE WHEN idempotency_key IS NULL THEN 1 END) as null_keys
FROM form_submissions;

-- Expected output:
-- total | unique_keys | null_keys
-- ------|-------------|----------
--  100  |     100     |     0
--
-- If null_keys > 0, migration failed - some rows don't have idempotency keys

-- ============================================
-- POST-MIGRATION CHECKLIST
-- ============================================
-- [ ] Run: SELECT * FROM form_submissions LIMIT 1; 
--     Verify idempotency_key column exists and has value
-- [ ] Run: SELECT COUNT(*) FROM form_submissions WHERE idempotency_key IS NULL;
--     Should return 0 (no null values)
-- [ ] Run: \d form_submissions
--     Verify UNIQUE constraint and indexes are present
-- [ ] Restart Flask server
-- [ ] Test: Submit form with idempotency_key, then retry with same key
--     Should return same submission_id (no duplicate)

-- ============================================
-- ROLLBACK (if needed)
-- ============================================
-- DROP INDEX idx_form_submissions_form_and_idempotency;
-- DROP INDEX idx_form_submissions_idempotency_key;
-- ALTER TABLE form_submissions DROP COLUMN idempotency_key;