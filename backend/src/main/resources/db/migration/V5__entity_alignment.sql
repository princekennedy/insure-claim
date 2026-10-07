-- InsureClaim Portal :: align the schema with the JPA entities.
--
-- Every entity extends BaseEntity (id, created_by, updated_by) or one of the
-- Auditable* variants, but the audit actor columns were never added to the
-- original migrations, and users carries its credential in password_hash while
-- the User entity maps "password". Hibernate runs with ddl-auto=validate, so
-- these gaps abort startup. All statements are guarded so the migration is
-- safe whether a column is absent, already present, or partially backfilled.

-- ── users :: canonical credential column ─────────────────────────────────
-- V1 shipped password_hash; the entity reads/writes "password". Rename when
-- that is all we have, otherwise backfill any NULL from the legacy hash.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password_hash'
        ) THEN
            ALTER TABLE users RENAME COLUMN password_hash TO password;
        ELSE
            ALTER TABLE users ADD COLUMN password VARCHAR(255);
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password_hash'
    ) THEN
        UPDATE users SET password = password_hash WHERE password IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'password'
    ) AND NOT EXISTS (SELECT 1 FROM users WHERE password IS NULL) THEN
        ALTER TABLE users ALTER COLUMN password SET NOT NULL;
    END IF;
END $$;

-- ── users :: password reset fields ───────────────────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMPTZ;

-- ── audit actor columns (BaseEntity.createdBy / BaseEntity.updatedBy) ────
ALTER TABLE users                ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE users                ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE refresh_tokens       ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE refresh_tokens       ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE vehicles             ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE vehicles             ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE policies             ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE policies             ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE claims               ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE claims               ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE claim_documents      ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE claim_documents      ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE claim_status_events  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE claim_status_events  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE claim_public_tokens  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE claim_public_tokens  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE kyc_verifications    ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE kyc_verifications    ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE garages              ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE garages              ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE repair_jobs          ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE repair_jobs          ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE garage_feedback      ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE garage_feedback      ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE fraud_alerts         ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE fraud_alerts         ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE chat_conversations   ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE chat_conversations   ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE chat_messages        ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE chat_messages        ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE support_concerns     ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE support_concerns     ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- ── kyc_verifications :: KycVerification.resubmissionAllowed ─────────────
ALTER TABLE kyc_verifications ADD COLUMN IF NOT EXISTS resubmission_allowed BOOLEAN NOT NULL DEFAULT FALSE;
