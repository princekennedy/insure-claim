'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  PageHeader,
  PageLoader,
} from '@/lib/components/ui';
import { getAdminRoles } from '@/lib/services/admin';
import type { RoleDefinition } from '@/lib/types';
import styles from '../management.module.css';

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleDefinition[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadRoles = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRoles(await getAdminRoles());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load the role catalog.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRoles();
  }, [loadRoles]);

  return (
    <section className={styles.page}>
      <PageHeader
        title="Roles"
        description="Review the role catalog and the permissions granted to each role."
      />

      {error && (
        <>
          <Alert type="error">{error}</Alert>
          <div className={styles.errorActions}>
            <Button type="button" variant="secondary" onClick={() => void loadRoles()} loading={loading}>
              Try again
            </Button>
          </div>
        </>
      )}

      {loading && !roles ? (
        <PageLoader message="Loading role catalog..." />
      ) : roles?.length === 0 ? (
        <EmptyState title="No roles available" description="The role catalog returned no entries." />
      ) : roles ? (
        <div className={styles.roleGrid}>
          {roles.map((role) => (
            <Card key={role.code} className={styles.roleCard}>
              <CardHeader className={styles.roleHeader}>
                <div>
                  <CardTitle className={styles.roleName}>{role.name}</CardTitle>
                  <p className={styles.roleCode}>{role.code}</p>
                </div>
                <Badge variant="secondary" size="sm">
                  {role.permissions.length} {role.permissions.length === 1 ? 'permission' : 'permissions'}
                </Badge>
              </CardHeader>
              <CardContent>
                <p className={styles.roleDescription}>{role.description || 'No description provided.'}</p>
                <h2 className={styles.permissionHeading}>Granted permissions</h2>
                {role.permissions.length === 0 ? (
                  <p className={styles.secondaryText}>No permissions are listed for this role.</p>
                ) : (
                  <ul className={styles.permissionList}>
                    {role.permissions.map((permission) => (
                      <li key={permission.code}>
                        <div className={styles.permissionName}>
                          <span>{permission.name}</span>
                          <span className={styles.permissionCode}>{permission.code}</span>
                        </div>
                        <p className={styles.permissionDescription}>
                          {permission.description || 'No description provided.'}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}
    </section>
  );
}
