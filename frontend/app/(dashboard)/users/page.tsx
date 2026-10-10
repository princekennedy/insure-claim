'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Dialog,
  EmptyState,
  Input,
  PageHeader,
  PageLoader,
  Pagination,
  Select,
} from '@/lib/components/ui';
import {
  createUser,
  deleteUser,
  getAdminRoles,
  getUser,
  getUsers,
  setUserEnabled,
  setUserRole,
  updateUser,
} from '@/lib/services/admin';
import type {
  PageResponse,
  UserDetail,
  UserSummary,
} from '@/lib/types';
import styles from '../management.module.css';

const PAGE_SIZE = 20;
const FALLBACK_ROLES = ['CUSTOMER', 'AGENT', 'INSURER_ADMIN', 'ADMIN'];

interface UserFilters {
  query: string;
  role: string;
}

interface CreateUserFormState {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  nic: string;
  role: string;
}

interface EditUserFormState {
  fullName: string;
  phone: string;
  nic: string;
  role: string;
  password: string;
}

const EMPTY_CREATE_FORM: CreateUserFormState = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  nic: '',
  role: 'CUSTOMER',
};

export default function UsersPage() {
  const [result, setResult] = useState<PageResponse<UserSummary> | null>(null);
  const [query, setQuery] = useState('');
  const [role, setRole] = useState('');
  const [appliedFilters, setAppliedFilters] = useState<UserFilters>({ query: '', role: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');
  const [roleOptions, setRoleOptions] = useState<string[]>(FALLBACK_ROLES);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateUserFormState>(EMPTY_CREATE_FORM);
  const [createError, setCreateError] = useState('');
  const [saving, setSaving] = useState(false);

  const [editUser, setEditUser] = useState<UserDetail | null>(null);
  const [editForm, setEditForm] = useState<EditUserFormState | null>(null);
  const [editError, setEditError] = useState('');

  function formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date);
  }

  const loadUsers = useCallback(async (page: number, filters: UserFilters) => {
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

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const data = await getAdminRoles(0, 100);
        if (!cancelled && data.content.length > 0) {
          setRoleOptions(data.content.map((entry) => entry.code));
        }
      } catch {
        // Fall back to the built-in codes; the server still validates every choice.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

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

  function handleDelete(user: UserSummary) {
    const confirmed = window.confirm(
      `Delete ${user.fullName}'s account? This cannot be undone.`,
    );
    if (!confirmed) return;
    void runUserAction(user.id, () => deleteUser(user.id));
  }

  function openCreate() {
    setCreateForm(EMPTY_CREATE_FORM);
    setCreateError('');
    setCreateOpen(true);
  }

  function openEdit(user: UserSummary) {
    setActionError('');
    setEditError('');
    setEditForm(null);
    setEditUser(null);
    void (async () => {
      setBusyId(user.id);
      try {
        const detail = await getUser(user.id);
        setEditUser(detail);
        setEditForm({
          fullName: detail.fullName,
          phone: detail.phone ?? '',
          nic: detail.nic ?? '',
          role: detail.role,
          password: '',
        });
      } catch (err) {
        setActionError(err instanceof Error ? err.message : 'Unable to load that account.');
      } finally {
        setBusyId(null);
      }
    })();
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (
      !createForm.email.trim() ||
      !createForm.password ||
      !createForm.fullName.trim() ||
      !createForm.role
    ) {
      setCreateError('Email, password, full name and role are required.');
      return;
    }
    setSaving(true);
    setCreateError('');
    try {
      await createUser({
        email: createForm.email.trim(),
        password: createForm.password,
        fullName: createForm.fullName.trim(),
        phone: createForm.phone.trim() || undefined,
        nic: createForm.nic.trim() || undefined,
        role: createForm.role,
      });
      setCreateOpen(false);
      await loadUsers(0, appliedFilters);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'The account could not be created.');
    } finally {
      setSaving(false);
    }
  }

  async function handleEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editUser || !editForm) return;
    if (!editForm.fullName.trim()) {
      setEditError('Full name is required.');
      return;
    }
    setSaving(true);
    setEditError('');
    try {
      await updateUser(editUser.id, {
        fullName: editForm.fullName.trim(),
        phone: editForm.phone.trim() || null,
        nic: editForm.nic.trim() || null,
        role: editForm.role,
        password: editForm.password || undefined,
      });
      setEditUser(null);
      setEditForm(null);
      await loadUsers(result?.page ?? 0, appliedFilters);
    } catch (err) {
      setEditError(err instanceof Error ? err.message : 'The account could not be saved.');
    } finally {
      setSaving(false);
    }
  }

  const roleSelectOptions = roleOptions.map((code) => ({ value: code, label: code }));

  return (
    <section className={styles.page}>
      <PageHeader
        title="Users"
        description="Browse the user directory, manage roles and create new accounts."
        actions={
          <Button type="button" onClick={openCreate}>
            New user
          </Button>
        }
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
            {roleOptions.map((roleCode) => (
              <option key={roleCode} value={roleCode}>{roleCode}</option>
            ))}
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
                            {[...new Set([user.role, ...roleOptions])].map((roleCode) => (
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
                          <div className={styles.rowActions}>
                            <Button
                              type="button"
                              size="sm"
                              variant="secondary"
                              loading={busyId === user.id}
                              onClick={() => openEdit(user)}
                            >
                              Edit
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant={user.enabled ? 'danger' : 'secondary'}
                              loading={busyId === user.id}
                              onClick={() => handleToggle(user)}
                            >
                              {user.enabled ? 'Disable' : 'Enable'}
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="danger"
                              loading={busyId === user.id}
                              onClick={() => handleDelete(user)}
                            >
                              Delete
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            label="User directory pages"
            loading={loading}
            onPageChange={(next) => void loadUsers(next, appliedFilters)}
          />
        </>
      ) : null}

      <Dialog
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        title="New user"
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="create-user-form" loading={saving}>
              Create
            </Button>
          </>
        }
      >
        <form id="create-user-form" className={styles.dialogForm} onSubmit={handleCreate}>
          {createError && <Alert type="error">{createError}</Alert>}
          <Input
            label="Full name"
            value={createForm.fullName}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, fullName: event.target.value }))}
            maxLength={160}
            required
          />
          <Input
            label="Email"
            type="email"
            value={createForm.email}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, email: event.target.value }))}
            required
          />
          <Input
            label="Password"
            type="password"
            value={createForm.password}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, password: event.target.value }))}
            hint="At least 10 characters with upper, lower, digit and symbol"
            autoComplete="new-password"
            required
          />
          <Input
            label="Phone"
            value={createForm.phone}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, phone: event.target.value }))}
            placeholder="+94 77 000 0000"
          />
          <Input
            label="NIC"
            value={createForm.nic}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, nic: event.target.value }))}
            maxLength={32}
          />
          <Select
            label="Role"
            value={createForm.role}
            options={roleSelectOptions}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, role: event.target.value }))}
          />
        </form>
      </Dialog>

      <Dialog
        isOpen={editUser !== null}
        onClose={() => setEditUser(null)}
        title={editUser ? `Edit ${editUser.fullName}` : 'Edit user'}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setEditUser(null)}>
              Cancel
            </Button>
            <Button type="submit" form="edit-user-form" loading={saving} disabled={!editForm}>
              Save
            </Button>
          </>
        }
      >
        {editForm ? (
          <form id="edit-user-form" className={styles.dialogForm} onSubmit={handleEdit}>
            {editError && <Alert type="error">{editError}</Alert>}
            <Input
              label="Full name"
              value={editForm.fullName}
              onChange={(event) => setEditForm((prev) => (prev ? { ...prev, fullName: event.target.value } : prev))}
              maxLength={160}
              required
            />
            <Input
              label="Email"
              value={editUser?.email ?? ''}
              readOnly
              hint="Email addresses cannot be changed here."
            />
            <Input
              label="Phone"
              value={editForm.phone}
              onChange={(event) => setEditForm((prev) => (prev ? { ...prev, phone: event.target.value } : prev))}
              placeholder="+94 77 000 0000"
            />
            <Input
              label="NIC"
              value={editForm.nic}
              onChange={(event) => setEditForm((prev) => (prev ? { ...prev, nic: event.target.value } : prev))}
              maxLength={32}
            />
            <Select
              label="Role"
              value={editForm.role}
              options={roleSelectOptions}
              onChange={(event) => setEditForm((prev) => (prev ? { ...prev, role: event.target.value } : prev))}
            />
            <Input
              label="Reset password"
              type="password"
              value={editForm.password}
              onChange={(event) => setEditForm((prev) => (prev ? { ...prev, password: event.target.value } : prev))}
              hint="Leave blank to keep the current password"
              autoComplete="new-password"
            />
          </form>
        ) : (
          <PageLoader message="Loading account..." />
        )}
      </Dialog>
    </section>
  );
}
