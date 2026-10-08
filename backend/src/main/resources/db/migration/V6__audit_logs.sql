-- ───────────────────────────────────────────────────────────────────────────
-- Audit trail: one row per invocation of an @Auditable endpoint.
-- Append-only; actor references the users table so the trail survives
-- account renames but drops cleanly if an account is deleted.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id             BIGSERIAL PRIMARY KEY,
    actor_id       BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    actor_email    VARCHAR(255),
    actor_role     VARCHAR(64),
    action         VARCHAR(64)   NOT NULL,
    description    VARCHAR(255)  NOT NULL DEFAULT '',
    entity_type    VARCHAR(64),
    entity_id      VARCHAR(64),
    request_method VARCHAR(8)    NOT NULL,
    request_path   VARCHAR(255)  NOT NULL,
    status         INT           NOT NULL,
    success        BOOLEAN       NOT NULL DEFAULT TRUE,
    detail         VARCHAR(512),
    ip_address     VARCHAR(64),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, created_at DESC);
