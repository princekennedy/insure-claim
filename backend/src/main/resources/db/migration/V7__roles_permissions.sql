-- Admin-managed roles and permission grants.
-- The four system roles (ADMIN, INSURER_ADMIN, AGENT, CUSTOMER) are seeded
-- with is_system = true and are never deleted from the UI; extra roles an
-- administrator creates live in the same table and grant authorization
-- through ROLE_STAFF (any non-CUSTOMER role is staff).
CREATE TABLE roles (
    code        VARCHAR(32)  PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    code        VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT ''
);

CREATE TABLE role_permissions (
    role_code       VARCHAR(32) NOT NULL REFERENCES roles (code) ON DELETE CASCADE,
    permission_code VARCHAR(64) NOT NULL REFERENCES permissions (code) ON DELETE RESTRICT,
    PRIMARY KEY (role_code, permission_code)
);

CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_code);
