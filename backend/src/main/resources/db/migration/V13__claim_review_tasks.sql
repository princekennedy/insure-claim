DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM roles WHERE code IN ('OFFICER', 'MANAGER')) THEN
        RAISE EXCEPTION 'Existing OFFICER or MANAGER role must be reconciled before installing claim review tasks';
    END IF;
END $$;

INSERT INTO roles (code, name, description, is_system)
VALUES
    ('OFFICER', 'Claims Officer', 'Review assigned claims', TRUE),
    ('MANAGER', 'Claims Manager', 'Assign claim review tasks', TRUE);

CREATE TABLE claim_review_tasks (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL UNIQUE REFERENCES claims (id) ON DELETE CASCADE,
    assignee_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    assigned_by_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE claim_review_task_assignments (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES claim_review_tasks (id) ON DELETE CASCADE,
    officer_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    actor_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    officer_label VARCHAR(160) NOT NULL,
    actor_label VARCHAR(160) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL
);

INSERT INTO claim_review_tasks (claim_id, created_at)
SELECT id, submitted_at FROM claims
WHERE status IN ('SUBMITTED', 'KYC_PENDING', 'UNDER_REVIEW', 'FRAUD_CHECK');

CREATE INDEX idx_claim_review_tasks_assignee ON claim_review_tasks (assignee_id);
CREATE INDEX idx_claim_review_task_assignments_task ON claim_review_task_assignments (task_id, assigned_at);
