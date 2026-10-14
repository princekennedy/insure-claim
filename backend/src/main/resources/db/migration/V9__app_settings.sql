-- Admin-managed runtime configuration. Each row overrides the environment /
-- application.properties default of the same key, so operators can tune the
-- portal from the Settings screen without a redeploy. The seeds below mirror
-- the defaults declared in security/Properties.kt.
CREATE TABLE app_settings (
    id            BIGSERIAL PRIMARY KEY,
    setting_group VARCHAR(32)  NOT NULL,
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value TEXT         NOT NULL DEFAULT '',
    data_type     VARCHAR(16)  NOT NULL DEFAULT 'STRING',
    description   VARCHAR(255) NOT NULL DEFAULT '',
    created_by    BIGINT,
    updated_by    BIGINT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_settings_group_key UNIQUE (setting_group, setting_key),
    CONSTRAINT ck_app_settings_data_type CHECK (data_type IN ('STRING', 'NUMBER', 'BOOLEAN'))
);

CREATE INDEX idx_app_settings_group ON app_settings (setting_group, setting_key);

INSERT INTO app_settings (setting_group, setting_key, setting_value, data_type, description) VALUES
    ('chatbot', 'enabled', 'true', 'BOOLEAN', 'Whether the AI assistant answers customer questions'),
    ('chatbot', 'model', 'claude-sonnet-5', 'STRING', 'Model used for assistant replies'),
    ('chatbot', 'max_tokens', '700', 'NUMBER', 'Maximum tokens in a single assistant reply'),
    ('chatbot', 'escalate_threshold', '0.65', 'NUMBER', 'Confidence below which a conversation escalates to a human'),
    ('claims', 'max_open_claims_per_customer', '5', 'NUMBER', 'Open claims a customer may have at once'),
    ('claims', 'settlement_window_days', '30', 'NUMBER', 'Days a settlement offer stays valid'),
    ('fraud', 'auto_alert_threshold', '60', 'NUMBER', 'Score at which a claim is automatically flagged for fraud review');
