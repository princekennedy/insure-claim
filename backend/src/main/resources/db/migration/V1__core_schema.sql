-- InsureClaim Portal :: core schema
-- Customers (policyholders), insurer staff, admins, their vehicles and motor policies.

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(160) NOT NULL,
    phone               VARCHAR(32),
    nic                 VARCHAR(32),
    role                VARCHAR(32)  NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_count  INTEGER      NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'AGENT', 'INSURER_ADMIN', 'ADMIN'))
);

CREATE INDEX idx_users_role ON users (role);

CREATE TABLE refresh_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash    VARCHAR(128) NOT NULL,
    issued_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked_at    TIMESTAMPTZ,
    user_agent    VARCHAR(255),
    ip_address    VARCHAR(64),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at);

CREATE TABLE vehicles (
    id                    BIGSERIAL PRIMARY KEY,
    owner_id              BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    registration_number   VARCHAR(32)  NOT NULL,
    make                  VARCHAR(80)  NOT NULL,
    model                 VARCHAR(80)  NOT NULL,
    year                  INTEGER      NOT NULL,
    color                 VARCHAR(40),
    chassis_number        VARCHAR(64),
    engine_number         VARCHAR(64),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vehicles_registration UNIQUE (registration_number),
    CONSTRAINT ck_vehicles_year CHECK (year BETWEEN 1900 AND 2100)
);

CREATE INDEX idx_vehicles_owner ON vehicles (owner_id);

CREATE TABLE policies (
    id               BIGSERIAL PRIMARY KEY,
    policy_number    VARCHAR(48)  NOT NULL,
    customer_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vehicle_id       BIGINT       NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    insurer_name     VARCHAR(160) NOT NULL DEFAULT 'Britam Insurance PLC',
    product_code     VARCHAR(64)  NOT NULL DEFAULT 'MOTOR_COMPREHENSIVE',
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    premium_amount   NUMERIC(12, 2) NOT NULL,
    sum_insured      NUMERIC(14, 2) NOT NULL,
    excess_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status           VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_policies_number UNIQUE (policy_number),
    CONSTRAINT ck_policies_dates CHECK (end_date > start_date),
    CONSTRAINT ck_policies_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'LAPSED')),
    CONSTRAINT uq_policies_vehicle UNIQUE (vehicle_id)
);

CREATE INDEX idx_policies_customer ON policies (customer_id);
CREATE INDEX idx_policies_status ON policies (status);
CREATE INDEX idx_policies_end_date ON policies (end_date);
