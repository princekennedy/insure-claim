-- InsureClaim Portal :: KYC verification, garages, repair jobs, feedback

CREATE TABLE kyc_verifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    claim_id            BIGINT       REFERENCES claims (id) ON DELETE CASCADE,
    document_type       VARCHAR(32)  NOT NULL,
    document_number     VARCHAR(64)  NOT NULL,
    full_name_on_doc    VARCHAR(160) NOT NULL,
    date_of_birth       DATE,
    front_image_path    VARCHAR(512),
    back_image_path     VARCHAR(512),
    selfie_path         VARCHAR(512),
    status              VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(255),
    confidence_score    NUMERIC(5, 4),
    verified_by         BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    verified_at         TIMESTAMPTZ,
    expires_at          DATE,
    submitted_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_kyc_user_document UNIQUE (user_id, document_type),
    CONSTRAINT ck_kyc_document_type CHECK (document_type IN
        ('NIC', 'PASSPORT', 'DRIVING_LICENSE')),
    CONSTRAINT ck_kyc_status CHECK (status IN
        ('PENDING', 'IN_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX idx_kyc_user ON kyc_verifications (user_id);
CREATE INDEX idx_kyc_claim ON kyc_verifications (claim_id);
CREATE INDEX idx_kyc_status ON kyc_verifications (status);

CREATE TABLE garages (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(24)  NOT NULL,
    name              VARCHAR(160) NOT NULL,
    address           VARCHAR(255) NOT NULL,
    city              VARCHAR(80)  NOT NULL,
    contact_phone     VARCHAR(32)  NOT NULL,
    contact_email     VARCHAR(255),
    panel_rating      NUMERIC(3, 2),
    rating_average    NUMERIC(3, 2) NOT NULL DEFAULT 0,
    rating_count      INTEGER      NOT NULL DEFAULT 0,
    complaint_count   INTEGER      NOT NULL DEFAULT 0,
    jobs_completed    INTEGER      NOT NULL DEFAULT 0,
    avg_turnaround_days NUMERIC(6, 2),
    is_panel_garage   BOOLEAN      NOT NULL DEFAULT TRUE,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    performance_status VARCHAR(24) NOT NULL DEFAULT 'GOOD',
    last_scored_at    TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_garages_code UNIQUE (code),
    CONSTRAINT ck_garages_performance CHECK (performance_status IN
        ('GOOD', 'WATCH', 'UNDERPERFORMING', 'SUSPENDED')),
    CONSTRAINT ck_garages_rating CHECK (rating_average BETWEEN 0 AND 5)
);

CREATE INDEX idx_garages_city ON garages (city);
CREATE INDEX idx_garages_performance ON garages (performance_status);

CREATE TABLE repair_jobs (
    id               BIGSERIAL PRIMARY KEY,
    claim_id         BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    garage_id        BIGINT       NOT NULL REFERENCES garages (id) ON DELETE RESTRICT,
    reference_code   VARCHAR(32)  NOT NULL,
    status           VARCHAR(24)  NOT NULL DEFAULT 'ASSIGNED',
    quoted_amount    NUMERIC(14, 2),
    approved_amount  NUMERIC(14, 2),
    final_amount     NUMERIC(14, 2),
    assigned_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    estimated_days   INTEGER,
    warranty_days    INTEGER      NOT NULL DEFAULT 90,
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_repair_jobs_reference UNIQUE (reference_code),
    CONSTRAINT uq_repair_jobs_claim UNIQUE (claim_id),
    CONSTRAINT ck_repair_jobs_status CHECK (status IN
        ('ASSIGNED', 'ACCEPTED', 'IN_REPAIR', 'AWAITING_PARTS', 'QUALITY_CHECK',
         'COMPLETED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_repair_jobs_garage ON repair_jobs (garage_id);
CREATE INDEX idx_repair_jobs_status ON repair_jobs (status);

CREATE TABLE garage_feedback (
    id                 BIGSERIAL PRIMARY KEY,
    claim_id           BIGINT      NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    garage_id          BIGINT      NOT NULL REFERENCES garages (id) ON DELETE CASCADE,
    repair_job_id      BIGINT      REFERENCES repair_jobs (id) ON DELETE SET NULL,
    customer_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    overall_rating     NUMERIC(3, 2) NOT NULL,
    quality_rating     NUMERIC(3, 2),
    timeliness_rating  NUMERIC(3, 2),
    price_fairness_rating NUMERIC(3, 2),
    staff_courtesy_rating NUMERIC(3, 2),
    comments           VARCHAR(1000),
    recommend_again    BOOLEAN,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_garage_feedback_claim UNIQUE (claim_id),
    CONSTRAINT ck_garage_feedback_overall CHECK (overall_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_garage_feedback_quality CHECK (quality_rating IS NULL OR quality_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_garage_feedback_timeliness CHECK (timeliness_rating IS NULL OR timeliness_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_garage_feedback_price CHECK (price_fairness_rating IS NULL OR price_fairness_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_garage_feedback_staff CHECK (staff_courtesy_rating IS NULL OR staff_courtesy_rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_garage_feedback_garage ON garage_feedback (garage_id, created_at DESC);
CREATE INDEX idx_garage_feedback_customer ON garage_feedback (customer_id);
