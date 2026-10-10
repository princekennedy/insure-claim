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
  Textarea,
} from '@/lib/components/ui';
import {
  createRole,
  deleteRole,
  getAdminRoles,
  getPermissions,
  updateRole,
} from '@/lib/services/admin';
import type { PageResponse, PermissionDefinition, RoleDefinition } from '@/lib/types';
import styles from '../management.module.css';

const PAGE_SIZE = 20;
const CODE_PATTERN = /^[A-Z][A-Z0-9_]{2,31}$/;

interface RoleFormState {
  code: string;
  name: string;
  description: string;
  permissionCodes: string[];
}

const EMPTY_FORM: RoleFormState = { code: '', name: '', description: '', permissionCodes: [] };

export default function RolesPage() {
  const [result, setResult] = useState<PageResponse<RoleDefinition> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [busyCode, setBusyCode] = useState<string | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<RoleDefinition | null>(null);
  const [form, setForm] = useState<RoleFormState>(EMPTY_FORM);
  const [permissions, setPermissions] = useState<PermissionDefinition[]>([]);
  const [permissionError, setPermissionError] = useState('');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadRoles = useCallback(async (targetPage: number) => {
    setLoading(true);
    setError('');
    try {
      let data = await getAdminRoles(targetPage, PAGE_SIZE);
      if (data.empty && data.page > 0) {
        data = await getAdminRoles(data.page - 1, PAGE_SIZE);
      }
      setResult(data);
      setPage(data.page);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load the role catalog.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRoles(0);
  }, [loadRoles]);

  async function ensurePermissions() {
    if (permissions.length > 0) return;
    try {
      setPermissions(await getPermissions());
    } catch (err) {
      setPermissionError(
        err instanceof Error ? err.message : 'Unable to load the permission catalog.',
      );
    }
  }

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setFormOpen(true);
    void ensurePermissions();
  }

  function openEdit(role: RoleDefinition) {
    setEditing(role);
    setForm({
      code: role.code,
      name: role.name,
      description: role.description,
      permissionCodes: role.permissions.map((permission) => permission.code),
    });
    setFormError('');
    setFormOpen(true);
    void ensurePermissions();
  }

  function togglePermission(code: string) {
    setForm((prev) => ({
      ...prev,
      permissionCodes: prev.permissionCodes.includes(code)
        ? prev.permissionCodes.filter((existing) => existing !== code)
        : [...prev.permissionCodes, code],
    }));
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const code = form.code.trim().toUpperCase();
    const name = form.name.trim();
    if (!editing && !CODE_PATTERN.test(code)) {
      setFormError('Code must be 3-32 characters (A-Z, 0-9 or underscore) starting with a letter.');
      return;
    }
    if (name.length < 2) {
      setFormError('Name must be at least 2 characters.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      if (editing) {
        await updateRole(editing.code, {
          name,
          description: form.description.trim(),
          permissionCodes: form.permissionCodes,
        });
      } else {
        await createRole({
          code,
          name,
          description: form.description.trim(),
          permissionCodes: form.permissionCodes,
        });
      }
      setFormOpen(false);
      await loadRoles(page);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'The role could not be saved.');
    } finally {
      setSaving(false);
    }
  }

  function handleDelete(role: RoleDefinition) {
    const confirmed = window.confirm(
      `Delete the ${role.name} role? Users with this role must be reassigned first.`,
    );
    if (!confirmed) return;
    void (async () => {
      setBusyCode(role.code);
      setActionError('');
      try {
        await deleteRole(role.code);
        await loadRoles(page);
      } catch (err) {
        setActionError(err instanceof Error ? err.message : 'The role could not be deleted.');
      } finally {
        setBusyCode(null);
      }
    })();
  }

  return (
    <section className={styles.page}>
      <PageHeader
        title="Roles"
        description="Create roles, review their permission grants and retire the ones you no longer need."
        actions={
          <Button type="button" onClick={openCreate}>
            New role
          </Button>
        }
      />

      {error && (
        <>
          <Alert type="error">{error}</Alert>
          <div className={styles.errorActions}>
            <Button type="button" variant="secondary" onClick={() => void loadRoles(page)} loading={loading}>
              Try again
            </Button>
          </div>
        </>
      )}

      {actionError && <Alert type="error">{actionError}</Alert>}

      {loading && !result ? (
        <PageLoader message="Loading role catalog..." />
      ) : result ? (
        <>
          <p className={styles.resultLine}>
            {result.totalElements} {result.totalElements === 1 ? 'role' : 'roles'} in the catalog
          </p>
          {result.empty ? (
            <EmptyState
              title="No roles available"
              description="Create the first role to get started."
            />
          ) : (
            <Card className={styles.tableCard} padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Role catalog</caption>
                  <thead>
                    <tr>
                      <th scope="col">Code</th>
                      <th scope="col">Name</th>
                      <th scope="col">Description</th>
                      <th scope="col">Permissions</th>
                      <th scope="col">Type</th>
                      <th scope="col">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.content.map((role) => (
                      <tr key={role.code}>
                        <td className={styles.primaryCell}>{role.code}</td>
                        <td>{role.name}</td>
                        <td>{role.description || '—'}</td>
                        <td>{role.permissions.length}</td>
                        <td>
                          <Badge variant={role.system ? 'secondary' : 'success'} size="sm">
                            {role.system ? 'System' : 'Custom'}
                          </Badge>
                        </td>
                        <td>
                          <div className={styles.rowActions}>
                            <Button
                              type="button"
                              size="sm"
                              variant="secondary"
                              onClick={() => openEdit(role)}
                            >
                              Edit
                            </Button>
                            {!role.system && (
                              <Button
                                type="button"
                                size="sm"
                                variant="danger"
                                loading={busyCode === role.code}
                                onClick={() => handleDelete(role)}
                              >
                                Delete
                              </Button>
                            )}
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
            label="Role catalog pages"
            loading={loading}
            onPageChange={(next) => void loadRoles(next)}
          />
        </>
      ) : null}

      <Dialog
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? `Edit ${editing.code}` : 'New role'}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setFormOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="role-form" loading={saving}>
              Save
            </Button>
          </>
        }
      >
        <form id="role-form" className={styles.dialogForm} onSubmit={handleSave}>
          {formError && <Alert type="error">{formError}</Alert>}
          {!editing && (
            <Input
              label="Code"
              value={form.code}
              onChange={(event) => setForm((prev) => ({ ...prev, code: event.target.value.toUpperCase() }))}
              placeholder="e.g. BRANCH_MANAGER"
              maxLength={32}
              required
            />
          )}
          <Input
            label="Name"
            value={form.name}
            onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
            placeholder="e.g. Branch Manager"
            maxLength={80}
            required
          />
          <Textarea
            label="Description"
            value={form.description}
            onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
            placeholder="What this role is responsible for"
            rows={3}
            maxLength={255}
          />
          <fieldset className={styles.fieldset}>
            <legend className={styles.filterLabel}>Granted permissions</legend>
            {permissionError ? (
              <Alert type="error">{permissionError}</Alert>
            ) : permissions.length === 0 ? (
              <p className={styles.secondaryText}>Loading permissions...</p>
            ) : (
              <div className={styles.permissionPicker}>
                {permissions.map((permission) => (
                  <label key={permission.code} className={styles.permissionOption}>
                    <input
                      type="checkbox"
                      checked={form.permissionCodes.includes(permission.code)}
                      onChange={() => togglePermission(permission.code)}
                    />
                    <span>{permission.name}</span>
                    <span className={styles.permissionOptionCode}>{permission.code}</span>
                  </label>
                ))}
              </div>
            )}
          </fieldset>
        </form>
      </Dialog>
    </section>
  );
}
