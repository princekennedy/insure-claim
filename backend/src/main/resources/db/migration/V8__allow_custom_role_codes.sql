-- Admins can create their own roles (see V7), so a user's role code is no
-- longer limited to the four system codes. Keep the column non-empty
-- instead of enumerating values that now live in the roles table.
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role <> '');
