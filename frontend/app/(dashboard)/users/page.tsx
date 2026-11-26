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
import { ApiError } from '@/lib/services/api';
import { getCurrentUser } from '@/lib/services/auth';
import type {
  PageResponse,
  UserDetail,
  UserSummary,
} from '@/lib/types';
import styles from '../management.module.css';
import createStyles from './create-user.module.css';

const PAGE_SIZE = 20;
const REQUIRED_ROLES = ['CUSTOMER', 'OFFICER', 'MANAGER'];

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
  const [roleOptions, setRoleOptions] = useState<string[]>([]);
  const [roleError, setRoleError] = useState('');
  const [viewerRole, setViewerRole] = useState<string | null>(null);
  const [viewerError, setViewerError] = useState('');

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
    void Promise.resolve().then(() => loadUsers(0, appliedFilters));
  }, [appliedFilters, loadUsers]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const codes: string[] = [];
        let page = 0;
        let last = false;
        while (!last) {
          const data = await getAdminRoles(page, 100);
          codes.push(...data.content.map((entry) => entry.code));
          last = data.last;
          page += 1;
        }
        if (!cancelled) {
          setRoleOptions(codes);
          if (codes.length === 0) setRoleError('No roles are available in the role catalog.');
        }
      } catch (err) {
        if (!cancelled) {
          setRoleError(err instanceof Error ? `Unable to load roles: ${err.message}` : 'Unable to load roles.');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((user) => {
        if (!cancelled) setViewerRole(user.role);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setViewerError(err instanceof Error ? `Unable to verify your account: ${err.message}` : 'Unable to verify your account.');
        }
      });
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
    setCreateForm({ ...EMPTY_CREATE_FORM, role: roleOptions.includes('CUSTOMER') ? 'CUSTOMER' : '' });
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
      setCreateError(
        err instanceof ApiError && err.status === 403
          ? 'Only a platform or insurer administrator can create user accounts. Sign in as an administrator or contact one.'
          : err instanceof Error ? err.message : 'The account could not be created.',
      );
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
  const missingRoles = REQUIRED_ROLES.filter((code) => !roleOptions.includes(code));

  return (
    <section className={styles.page}>
      <PageHeader
        title="Users"
        description="Browse the user directory, manage roles and create new accounts."
        actions={
          <Button type="button" onClick={openCreate} disabled={viewerRole !== 'ADMIN' && viewerRole !== 'INSURER_ADMIN'}>
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

      {viewerError && <Alert type="error">{viewerError}</Alert>}
      {viewerRole && viewerRole !== 'ADMIN' && viewerRole !== 'INSURER_ADMIN' && (
        <Alert type="info">Only a platform or insurer administrator can create user accounts.</Alert>
      )}
      {roleError && <Alert type="error">{roleError}</Alert>}
      {!roleError && roleOptions.length > 0 && missingRoles.length > 0 && (
        <Alert type="warning">
          The role catalog is missing {missingRoles.join(', ')}. Update the backend and apply its database migrations before creating those accounts.
        </Alert>
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
                            disabled={busyId === user.id || roleOptions.length === 0}
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
                              className={`${styles.rowAction} ${styles.editAction}`}
                              loading={busyId === user.id}
                              onClick={() => openEdit(user)}
                            >
                              Edit
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="secondary"
                              className={`${styles.rowAction} ${user.enabled ? styles.disableAction : styles.enableAction}`}
                              loading={busyId === user.id}
                              onClick={() => handleToggle(user)}
                            >
                              {user.enabled ? 'Disable' : 'Enable'}
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="secondary"
                              className={`${styles.rowAction} ${styles.deleteAction}`}
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
        size="lg"
        classNames={{
          overlay: createStyles.overlay,
          dialog: createStyles.dialog,
          header: createStyles.header,
          title: createStyles.title,
          closeButton: createStyles.closeButton,
          content: createStyles.content,
          footer: createStyles.footer,
        }}
        footer={
          <>
            <span className={createStyles.requiredNote}><span aria-hidden="true">*</span> Required fields</span>
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="create-user-form" loading={saving} disabled={roleOptions.length === 0}>
              Create user
            </Button>
          </>
        }
      >
        <form id="create-user-form" className={createStyles.form} onSubmit={handleCreate}>
          {createError && <Alert type="error">{createError}</Alert>}
          <div className={createStyles.intro}>
            <span className={createStyles.introIcon} aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="10" cy="8" r="3.25" />
                <path d="M3.75 19v-1.1a6.25 6.25 0 0 1 11.6-3.23M18.5 15v6M15.5 18h6" />
              </svg>
            </span>
            <div>
              <h3 className={createStyles.introTitle}>Set up an account</h3>
              <p className={createStyles.introText}>Add their details and choose the right access for their work.</p>
            </div>
          </div>

          <section className={createStyles.section} aria-labelledby="create-identity-heading">
            <div className={createStyles.sectionHeading}>
              <div>
                <h3 id="create-identity-heading">Account details</h3>
                <p>Tell us who this account belongs to.</p>
              </div>
              <span>01</span>
            </div>
            <div className={createStyles.fields}>
              <Input
                label="Full name"
                value={createForm.fullName}
                onChange={(event) => setCreateForm((prev) => ({ ...prev, fullName: event.target.value }))}
                maxLength={160}
                required
                aria-required="true"
              />
              <Input
                label="Email"
                type="email"
                value={createForm.email}
                onChange={(event) => setCreateForm((prev) => ({ ...prev, email: event.target.value }))}
                required
                aria-required="true"
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
            </div>
          </section>

          <section className={createStyles.section} aria-labelledby="create-access-heading">
            <div className={createStyles.sectionHeading}>
              <div>
                <h3 id="create-access-heading">Access &amp; security</h3>
                <p>Choose a role and secure their sign-in.</p>
              </div>
              <span>02</span>
            </div>
            <div className={createStyles.fields}>
              <Select
                label="Role *"
                value={createForm.role}
                options={roleSelectOptions}
                placeholder="Select a role"
                required
                disabled={roleOptions.length === 0}
                onChange={(event) => setCreateForm((prev) => ({ ...prev, role: event.target.value }))}
              />
              <Input
                label="Password"
                type="password"
                value={createForm.password}
                onChange={(event) => setCreateForm((prev) => ({ ...prev, password: event.target.value }))}
                hint="At least 10 characters with upper, lower, digit and symbol"
                autoComplete="new-password"
                required
                aria-required="true"
              />
            </div>
          </section>
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
              options={[...new Set([editForm.role, ...roleOptions])].map((code) => ({ value: code, label: code }))}
              disabled={roleOptions.length === 0}
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
