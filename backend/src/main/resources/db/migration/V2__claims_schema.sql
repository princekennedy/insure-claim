-- InsureClaim Portal :: claims, documents and status timeline

CREATE TABLE claims (
    id                    BIGSERIAL PRIMARY KEY,
    claim_number          VARCHAR(48)  NOT NULL,
    policy_id             BIGINT       NOT NULL REFERENCES policies (id) ON DELETE RESTRICT,
    customer_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    vehicle_id            BIGINT       NOT NULL REFERENCES vehicles (id) ON DELETE RESTRICT,
    incident_type         VARCHAR(32)  NOT NULL,
    incident_date         TIMESTAMPTZ  NOT NULL,
    incident_location     VARCHAR(255),
    description           TEXT         NOT NULL,
    estimated_amount      NUMERIC(14, 2),
    approved_amount       NUMERIC(14, 2),
    excess_paid           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status                VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED',
    requires_kyc          BOOLEAN      NOT NULL DEFAULT TRUE,
    fraud_score           INTEGER      NOT NULL DEFAULT 0,
    is_fraud_flagged      BOOLEAN      NOT NULL DEFAULT FALSE,
    reported_by_police    BOOLEAN      NOT NULL DEFAULT FALSE,
    third_party_involved  BOOLEAN      NOT NULL DEFAULT FALSE,
    submitted_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    settled_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_claims_number UNIQUE (claim_number),
    CONSTRAINT ck_claims_type CHECK (incident_type IN
        ('ACCIDENT', 'THEFT', 'FIRE', 'FLOOD', 'GLASS', 'WINDSCREEN', 'OTHER')),
    CONSTRAINT ck_claims_status CHECK (status IN
        ('DRAFT', 'SUBMITTED', 'KYC_PENDING', 'UNDER_REVIEW', 'FRAUD_CHECK',
         'APPROVED', 'REJECTED', 'GARAGE_ASSIGNED', 'IN_REPAIR', 'READY_FOR_PICKUP',
         'SETTLED', 'CLOSED', 'WITHDRAWN')),
    CONSTRAINT ck_claims_fraud_score CHECK (fraud_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_claims_customer ON claims (customer_id);
CREATE INDEX idx_claims_policy ON claims (policy_id);
CREATE INDEX idx_claims_status ON claims (status);
CREATE INDEX idx_claims_submitted_at ON claims (submitted_at DESC);
CREATE INDEX idx_claims_fraud ON claims (is_fraud_flagged) WHERE is_fraud_flagged = TRUE;

CREATE TABLE claim_documents (
    id             BIGSERIAL PRIMARY KEY,
    claim_id       BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    document_type  VARCHAR(40)  NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(120) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    storage_path   VARCHAR(512) NOT NULL,
    checksum       VARCHAR(64),
    uploaded_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_claim_documents_type CHECK (document_type IN
        ('DAMAGE_PHOTO', 'POLICE_REPORT', 'REPAIR_QUOTE', 'INVOICE',
         'OWNERSHIP_DOCUMENT', 'RECEIPT', 'OTHER')),
    CONSTRAINT ck_claim_documents_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_claim_documents_claim ON claim_documents (claim_id);

CREATE TABLE claim_status_events (
    id           BIGSERIAL PRIMARY KEY,
    claim_id     BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    from_status  VARCHAR(32),
    to_status    VARCHAR(32)  NOT NULL,
    note         VARCHAR(500),
    actor_id     BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    actor_label  VARCHAR(160) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_claim_status_events_claim ON claim_status_events (claim_id, occurred_at);

-- Allows public tracking by claim number without exposing sequential ids.
CREATE TABLE claim_public_tokens (
    claim_id    BIGINT      PRIMARY KEY REFERENCES claims (id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_claim_public_tokens_token UNIQUE (token)
);
