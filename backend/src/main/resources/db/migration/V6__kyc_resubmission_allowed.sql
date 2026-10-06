-- A rejected verification can either be closed for good or left open so the
-- customer can correct it and resubmit. The status alone cannot express both,
-- so the reviewer's intent is persisted alongside it.
ALTER TABLE kyc_verifications
    ADD COLUMN resubmission_allowed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN kyc_verifications.resubmission_allowed IS
    'True when staff rejected the submission but allow the customer to resubmit that document type.';