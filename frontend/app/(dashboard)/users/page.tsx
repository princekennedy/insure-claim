'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  PageHeader,
  PageLoader,
} from '@/lib/components/ui';
import { getUsers, setUserEnabled, setUserRole } from '@/lib/services/admin';
import type { PageResponse, UserSummary } from '@/lib/types';
import styles from '../management.module.css';

const PAGE_SIZE = 20;
const ROLES = ['ADMIN', 'INSURER_ADMIN', 'AGENT', 'CUSTOMER'];

export default function UsersPage() {
  const [result, setResult] = useState<PageResponse<UserSummary> | null>(null);
  const [query, setQuery] = useState('');
  const [role, setRole] = useState('');
  const [appliedFilters, setAppliedFilters] = useState({ query: '', role: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');

  function formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date);
  }

  const loadUsers = useCallback(async (page: number, filters: typeof appliedFilters) => {
    setLoading(true);
    setError('');
    try {
      setResult(await getUsers(page, PAGE_SIZE, {
        query: filters.query || undefined,
        role: filters.role || undefined,
      }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load the user directory.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers(0, appliedFilters);
  }, [appliedFilters, loadUsers]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAppliedFilters({ query: query.trim(), role });
  }

  function clearFilters() {
    setQuery('');
    setRole('');
    setAppliedFilters({ query: '', role: '' });
  }

  async function runUserAction(userId: number, action: () => Promise<unknown>) {
    setBusyId(userId);
    setActionError('');
    try {
      await action();
      await loadUsers(result?.page ?? 0, appliedFilters);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'The change could not be saved.');
    } finally {
      setBusyId(null);
    }
  }

  function handleToggle(user: UserSummary) {
    void runUserAction(user.id, () => setUserEnabled(user.id, !user.enabled));
  }

  function handleRoleChange(user: UserSummary, nextRole: string) {
    if (nextRole === user.role) return;
    const confirmed = window.confirm(
      `Change ${user.fullName}'s role from ${user.role} to ${nextRole}?`,
    );
    if (!confirmed) return;
    void runUserAction(user.id, () => setUserRole(user.id, nextRole));
  }

  return (
    <section className={styles.page}>
      <PageHeader
        title="Users"
        description="Browse the user directory and review account roles."
      />

      <form className={styles.filterBar} onSubmit={handleSearch} aria-label="Filter users">
        <div className={styles.filterField}>
          <label className={styles.filterLabel} htmlFor="user-search">Search users</label>
          <input
            className={styles.filterInput}
            id="user-search"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by name or email"
          />
        </div>
        <div className={styles.filterFieldNarrow}>
          <label className={styles.filterLabel} htmlFor="user-role">Role</label>
          <select
            className={styles.filterSelect}
            id="user-role"
            value={role}
            onChange={(event) => setRole(event.target.value)}
          >
            <option value="">All roles</option>
            {ROLES.map((roleCode) => <option key={roleCode} value={roleCode}>{roleCode}</option>)}
          </select>
        </div>
        <div className={styles.filterActions}>
          <Button type="submit" loading={loading}>Apply</Button>
          <Button type="button" variant="secondary" onClick={clearFilters}>Clear</Button>
        </div>
      </form>

      {error && (
        <>
          <Alert type="error">{error}</Alert>
          <div className={styles.errorActions}>
            <Button
              type="button"
              variant="secondary"
              onClick={() => void loadUsers(result?.page ?? 0, appliedFilters)}
              loading={loading}
            >
              Try again
            </Button>
          </div>
        </>
      )}

      {actionError && (
        <Alert type="error">{actionError}</Alert>
      )}

      {loading && !result ? (
        <PageLoader message="Loading users..." />
      ) : result ? (
        <>
          <p className={styles.resultLine}>
            {result.totalElements} {result.totalElements === 1 ? 'user' : 'users'}
            {appliedFilters.query || appliedFilters.role ? ' match these filters' : ' in directory'}
          </p>
          {result.empty ? (
            <EmptyState
              title="No users found"
              description="No accounts match the selected search and role filters."
            />
          ) : (
            <Card className={styles.tableCard} padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">User directory</caption>
                  <thead>
                    <tr>
                      <th scope="col">Name</th>
                      <th scope="col">Email</th>
                      <th scope="col">Role</th>
                      <th scope="col">Status</th>
                      <th scope="col">Last login</th>
                      <th scope="col">User ID</th>
                      <th scope="col">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.content.map((user) => (
                      <tr key={user.id}>
                        <td className={styles.primaryCell}>{user.fullName}</td>
                        <td>{user.email}</td>
                        <td>
                          <select
                            className={styles.roleSelect}
                            value={user.role}
                            disabled={busyId === user.id}
                            aria-label={`Role for ${user.fullName}`}
                            onChange={(event) => handleRoleChange(user, event.target.value)}
                          >
                            {ROLES.map((roleCode) => (
                              <option key={roleCode} value={roleCode}>{roleCode}</option>
                            ))}
                          </select>
                        </td>
                        <td>
                          <Badge variant={user.enabled ? 'success' : 'danger'} size="sm">
                            {user.enabled ? 'Active' : 'Disabled'}
                          </Badge>
                        </td>
                        <td>{user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Never'}</td>
                        <td>{user.id}</td>
                        <td>
                          <Button
                            type="button"
                            size="sm"
                            variant={user.enabled ? 'danger' : 'secondary'}
                            loading={busyId === user.id}
                            onClick={() => handleToggle(user)}
                          >
                            {user.enabled ? 'Disable' : 'Enable'}
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
          {result.totalPages > 1 && (
            <nav className={styles.pagination} aria-label="User directory pages">
              <span className={styles.pageCount}>
                Page {result.page + 1} of {result.totalPages}
              </span>
              <div className={styles.paginationActions}>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={result.first || loading}
                  onClick={() => void loadUsers(result.page - 1, appliedFilters)}
                >
                  Previous
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={result.last || loading}
                  onClick={() => void loadUsers(result.page + 1, appliedFilters)}
                >
                  Next
                </Button>
              </div>
            </nav>
          )}
        </>
      ) : null}
    </section>
  );
}
