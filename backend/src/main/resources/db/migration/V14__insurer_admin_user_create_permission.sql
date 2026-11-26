INSERT INTO role_permissions (role_code, permission_code)
SELECT roles.code, permissions.code
FROM roles CROSS JOIN permissions
WHERE roles.code = 'INSURER_ADMIN' AND permissions.code = 'USER_CREATE'
ON CONFLICT DO NOTHING;
