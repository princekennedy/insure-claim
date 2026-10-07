-- InsureClaim Portal :: fraud alerts, AI chatbot conversations, concern escalation

CREATE TABLE fraud_alerts (
    id            BIGSERIAL PRIMARY KEY,
    created_by    BIGINT,
    updated_by    BIGINT,
    claim_id      BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    rule_code     VARCHAR(48)  NOT NULL,
    category      VARCHAR(32)  NOT NULL,
    severity      VARCHAR(16)  NOT NULL,
    score_delta   INTEGER      NOT NULL DEFAULT 0,
    description   VARCHAR(500) NOT NULL,
    status        VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    resolution    VARCHAR(500),
    reviewed_by   BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_fraud_alerts_claim_rule UNIQUE (claim_id, rule_code),
    CONSTRAINT ck_fraud_alerts_category CHECK (category IN
        ('VELOCITY', 'AMOUNT', 'DOCUMENT', 'HISTORY', 'IDENTITY', 'BEHAVIOURAL', 'GARAGE')),
    CONSTRAINT ck_fraud_alerts_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_fraud_alerts_status CHECK (status IN
        ('OPEN', 'UNDER_REVIEW', 'CONFIRMED', 'DISMISSED', 'RESOLVED'))
);

CREATE INDEX idx_fraud_alerts_claim ON fraud_alerts (claim_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts (status);
CREATE INDEX idx_fraud_alerts_severity ON fraud_alerts (severity, created_at DESC);

CREATE TABLE chat_conversations (
    id            BIGSERIAL PRIMARY KEY,
    created_by    BIGINT,
    updated_by    BIGINT,
    user_id       BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    reference     VARCHAR(48)  NOT NULL,
    claim_id      BIGINT       REFERENCES claims (id) ON DELETE SET NULL,
    status        VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    messages_count INTEGER     NOT NULL DEFAULT 0,
    escalated     BOOLEAN      NOT NULL DEFAULT FALSE,
    escalated_at  TIMESTAMPTZ,
    started_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ended_at      TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_conversations_reference UNIQUE (reference),
    CONSTRAINT ck_chat_conversations_status CHECK (status IN
        ('OPEN', 'ESCALATED', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX idx_chat_conversations_user ON chat_conversations (user_id, started_at DESC);
CREATE INDEX idx_chat_conversations_escalated ON chat_conversations (escalated)
    WHERE escalated = TRUE;

CREATE TABLE chat_messages (
    id                 BIGSERIAL PRIMARY KEY,
    created_by    BIGINT,
    updated_by    BIGINT,
    conversation_id    BIGINT       NOT NULL REFERENCES chat_conversations (id) ON DELETE CASCADE,
    sender             VARCHAR(16)  NOT NULL,
    content            TEXT         NOT NULL,
    intent             VARCHAR(48),
    confidence         NUMERIC(5, 4),
    escalate_suggested BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_chat_messages_sender CHECK (sender IN ('CUSTOMER', 'ASSISTANT', 'AGENT'))
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages (conversation_id, created_at);

-- Concerns raised through the chatbot that need a human response.
CREATE TABLE support_concerns (
    id                 BIGSERIAL PRIMARY KEY,
    created_by    BIGINT,
    updated_by    BIGINT,
    conversation_id    BIGINT       REFERENCES chat_conversations (id) ON DELETE SET NULL,
    user_id            BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    claim_id           BIGINT       REFERENCES claims (id) ON DELETE SET NULL,
    category           VARCHAR(32)  NOT NULL,
    severity           VARCHAR(16)  NOT NULL,
    summary            VARCHAR(500) NOT NULL,
    status             VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    assigned_to        BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    resolved_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_support_concerns_category CHECK (category IN
        ('CLAIM_DELAY', 'PAYOUT', 'KYC', 'GARAGE_QUALITY', 'POLICY', 'FRAUD_FLAG',
         'COMPLAINT', 'TECHNICAL', 'OTHER')),
    CONSTRAINT ck_support_concerns_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_support_concerns_status CHECK (status IN
        ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_support_concerns_status ON support_concerns (status, created_at DESC);
CREATE INDEX idx_support_concerns_claim ON support_concerns (claim_id);
