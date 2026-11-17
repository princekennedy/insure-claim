-- Company knowledge is a catalogue, not a single settings value. Each row is
-- one article the assistant can cite; operators add, edit and retire them from
-- Settings > Knowledge base without touching the AI provider configuration.
CREATE TABLE knowledge_entries (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(160) NOT NULL,
    category    VARCHAR(64)  NOT NULL DEFAULT 'General',
    content     TEXT         NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  BIGINT,
    updated_by  BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_entries_enabled ON knowledge_entries (enabled);
CREATE INDEX idx_knowledge_entries_updated ON knowledge_entries (updated_at DESC);

-- Move the one-off company knowledge blob (V10) into the catalogue so no
-- content is lost, then remove it from the AI provider settings group.
INSERT INTO knowledge_entries (title, category, content, enabled)
SELECT 'Company information', 'General', setting_value, TRUE
FROM app_settings
WHERE setting_group = 'chatbot'
  AND setting_key = 'company_info'
  AND btrim(setting_value) <> '';

DELETE FROM app_settings
WHERE setting_group = 'chatbot'
  AND setting_key = 'company_info';

INSERT INTO permissions (code, name, description) VALUES
    ('SETTINGS_READ', 'Read Settings', 'View portal settings'),
    ('SETTINGS_UPDATE', 'Update Settings', 'Change portal settings'),
    ('KNOWLEDGE_READ', 'Read Knowledge Base', 'View assistant knowledge articles'),
    ('KNOWLEDGE_MANAGE', 'Manage Knowledge Base', 'Create, edit and retire assistant knowledge articles')
ON CONFLICT (code) DO NOTHING;

-- Existing installations already have their system roles; fresh installations
-- get the same grants from seed/role_permissions.json after Flyway runs.
INSERT INTO role_permissions (role_code, permission_code)
SELECT r.code, p.code
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'INSURER_ADMIN')
  AND p.code IN ('SETTINGS_READ', 'SETTINGS_UPDATE', 'KNOWLEDGE_READ', 'KNOWLEDGE_MANAGE')
ON CONFLICT (role_code, permission_code) DO NOTHING;
