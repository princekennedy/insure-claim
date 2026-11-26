'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Alert, Badge, Button, Dialog, PageLoader } from '@/lib/components/ui';
import { assignTask, claimTask, getTaskOfficers, getTasks } from '@/lib/services/tasks';
import { formatDateTime } from '@/lib/types/claim';
import type { ClaimTask, TaskOfficer } from '@/lib/types/task';
import type { QueueStatus } from './tasks-overview';
import styles from './tasks.module.css';

function message(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

function dateLabel(value: string): string {
  return Number.isNaN(new Date(value).getTime()) ? value : formatDateTime(value);
}

function statusVariant(status: string): 'success' | 'danger' | 'info' {
  if (status === 'SETTLED' || status === 'CLOSED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'danger';
  return 'info';
}

export function TaskQueue({ role, onStatusChange }: {
  role: 'OFFICER' | 'MANAGER' | 'ADMIN' | 'INSURER_ADMIN';
  onStatusChange: (status: QueueStatus) => void;
}) {
  const [tasks, setTasks] = useState<ClaimTask[]>([]);
  const [officers, setOfficers] = useState<TaskOfficer[]>([]);
  const [loading, setLoading] = useState(true);
  const [officersLoading, setOfficersLoading] = useState(role === 'MANAGER');
  const [error, setError] = useState('');
  const [officersError, setOfficersError] = useState('');
  const [actionErrors, setActionErrors] = useState<Record<number, string>>({});
  const [selected, setSelected] = useState<Record<number, string>>({});
  const [pending, setPending] = useState<Record<number, boolean>>({});
  const [announcement, setAnnouncement] = useState('');
  const [openTaskId, setOpenTaskId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    getTasks()
      .then((data) => {
        if (active) {
          setTasks(data);
          onStatusChange(data.length ? 'has-tasks' : 'empty');
        }
      })
      .catch((err) => {
        if (active) {
          setError(message(err, 'Unable to load tasks.'));
          onStatusChange('error');
        }
      })
      .finally(() => { if (active) setLoading(false); });
    if (role === 'MANAGER') {
      getTaskOfficers()
        .then((data) => { if (active) setOfficers(data); })
        .catch((err) => { if (active) setOfficersError(message(err, 'Unable to load officers.')); })
        .finally(() => { if (active) setOfficersLoading(false); });
    }
    return () => { active = false; };
  }, [role, onStatusChange]);

  async function reloadTasks() {
    onStatusChange('loading');
    setLoading(true);
    setError('');
    try {
      const data = await getTasks();
      setTasks(data);
      onStatusChange(data.length ? 'has-tasks' : 'empty');
      setActionErrors({});
    } catch (err) {
      setError(message(err, 'Unable to load tasks.'));
      onStatusChange('error');
    } finally {
      setLoading(false);
    }
  }

  async function reloadOfficers() {
    setOfficersLoading(true);
    setOfficersError('');
    try {
      setOfficers(await getTaskOfficers());
    } catch (err) {
      setOfficersError(message(err, 'Unable to load officers.'));
    } finally {
      setOfficersLoading(false);
    }
  }

  async function updateTask(task: ClaimTask) {
    const officerId = Number(selected[task.id]);
    if (role === 'OFFICER' && task.assigneeId != null) return;
    if (role === 'MANAGER' && (!officerId || officerId === task.assigneeId)) return;
    setPending((current) => ({ ...current, [task.id]: true }));
    setActionErrors((current) => ({ ...current, [task.id]: '' }));
    setAnnouncement('');
    try {
      const updated = role === 'OFFICER'
        ? await claimTask(task.id)
        : await assignTask(task.id, officerId);
      setTasks((current) => current.map((item) => item.id === task.id ? updated : item));
      setSelected((current) => ({ ...current, [task.id]: '' }));
      setAnnouncement(`${task.claimNumber} ${role === 'OFFICER' ? 'claimed' : 'assignment updated'}.`);
    } catch (err) {
      setActionErrors((current) => ({
        ...current,
        [task.id]: message(err, 'Unable to update this task. Please try again.'),
      }));
    } finally {
      setPending((current) => ({ ...current, [task.id]: false }));
    }
  }

  const unassigned = tasks.filter((task) => task.assigneeId == null).length;
  const updating = Object.values(pending).some(Boolean);
  const openTask = tasks.find((task) => task.id === openTaskId);

  return (
    <div className={styles.page}>
      <p className={styles.srOnly} role="status">{announcement}</p>

      {error && (
        <Alert type="error">
          <div className={styles.alertContent}>
            <span>{error}</span>
            <Button type="button" variant="secondary" size="sm" disabled={updating} onClick={() => void reloadTasks()}>Try again</Button>
          </div>
        </Alert>
      )}
      {role === 'MANAGER' && officersError && (
        <Alert type="error">
          <div className={styles.alertContent}>
            <span>Officers could not be loaded: {officersError}</span>
            <Button type="button" variant="secondary" size="sm" onClick={() => void reloadOfficers()}>Try again</Button>
          </div>
        </Alert>
      )}

      {loading && tasks.length === 0 ? <PageLoader message="Loading tasks..." /> : (
        <>
          {tasks.length > 0 ? (
            <section className={styles.queue} aria-labelledby="queue-heading" aria-busy={loading}>
              <div className={styles.queueHeader}>
                <div>
                  <h2 id="queue-heading">Claim review queue</h2>
                  <p>{tasks.length} {tasks.length === 1 ? 'task' : 'tasks'} · {unassigned} unassigned</p>
                </div>
                <Button type="button" variant="secondary" size="sm" loading={loading} disabled={updating} onClick={() => void reloadTasks()}>
                  Refresh
                </Button>
              </div>
              <ul className={styles.list}>
                {tasks.map((task) => (
                  <li className={styles.item} key={task.id}>
                    <button type="button" className={styles.taskSummary} aria-haspopup="dialog" disabled={loading || updating} onClick={() => setOpenTaskId(task.id)}>
                      <span>Claim</span>
                      <strong>{task.customerName}</strong>
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      )}
      <Dialog
        isOpen={!!openTask}
        onClose={() => { if (!updating) setOpenTaskId(null); }}
        title="Claim review"
        size="lg"
        closeOnOverlay={!updating}
        showCloseButton={!updating}
        classNames={{ content: styles.dialogContent }}
        footer={<Button type="button" variant="secondary" disabled={updating} onClick={() => setOpenTaskId(null)}>Close</Button>}
      >
        {openTask && (
          <div className={styles.itemDetails}>
            <div>
              <div className={styles.claimHeading}>
                <Link className={styles.claimLink} href={`/claims/${openTask.claimId}`}>
                  {openTask.claimNumber}
                  <span className={styles.linkArrow} aria-hidden="true">↗</span>
                </Link>
                <Badge variant={statusVariant(openTask.claimStatus)} size="sm">
                  {openTask.claimStatus.replace(/_/g, ' ')}
                </Badge>
              </div>
              <dl className={styles.details}>
                <div><dt>Claimant</dt><dd>{openTask.customerName}</dd></div>
                <div><dt>Vehicle</dt><dd>{openTask.vehicleRegistration}</dd></div>
                <div><dt>Submitted</dt><dd>{dateLabel(openTask.submittedAt)}</dd></div>
              </dl>
            </div>
            <div className={styles.assignment}>
              <span className={styles.assignmentLabel}>Assignment</span>
              {openTask.assigneeId == null ? (
                <span className={styles.unassigned}>Unassigned</span>
              ) : (
                <>
                  <span className={styles.assignee}>{openTask.assigneeName || `Officer #${openTask.assigneeId}`}</span>
                  {(openTask.assignedAt || openTask.assignedByName) && (
                    <span className={styles.assignmentMeta}>
                      {openTask.assignedByName && `By ${openTask.assignedByName}`}
                      {openTask.assignedAt && `${openTask.assignedByName ? ' · ' : ''}${dateLabel(openTask.assignedAt)}`}
                    </span>
                  )}
                </>
              )}
              {role === 'OFFICER' && openTask.assigneeId == null && (
                <Button type="button" size="sm" aria-label={`Claim task for ${openTask.claimNumber}`} loading={pending[openTask.id]} disabled={loading} onClick={() => void updateTask(openTask)}>
                  Claim task
                </Button>
              )}
              {role === 'MANAGER' && (
                <>
                  {officersError && (
                    <Alert type="error">
                      <div className={styles.alertContent}>
                        <span>Officers could not be loaded: {officersError}</span>
                        <Button type="button" variant="secondary" size="sm" onClick={() => void reloadOfficers()}>Try again</Button>
                      </div>
                    </Alert>
                  )}
                  <div className={styles.assignControls}>
                    <label className={styles.srOnly} htmlFor={`officer-${openTask.id}`}>Officer for {openTask.claimNumber}</label>
                    <select
                      id={`officer-${openTask.id}`}
                      className={styles.select}
                      value={selected[openTask.id] || ''}
                      disabled={loading || officersLoading || !!officersError || pending[openTask.id] || officers.length === 0}
                      onChange={(event) => setSelected((current) => ({ ...current, [openTask.id]: event.target.value }))}
                    >
                      <option value="">{officersLoading ? 'Loading officers…' : officers.length === 0 ? 'No officers available' : 'Select officer'}</option>
                      {officers.map((officer) => <option key={officer.id} value={officer.id}>{officer.fullName}</option>)}
                    </select>
                    <Button
                      type="button"
                      size="sm"
                      aria-label={`${openTask.assigneeId == null ? 'Assign' : 'Reassign'} task for ${openTask.claimNumber}`}
                      loading={pending[openTask.id]}
                      disabled={loading || officersLoading || officers.length === 0 || !selected[openTask.id] || Number(selected[openTask.id]) === openTask.assigneeId || !!officersError}
                      onClick={() => void updateTask(openTask)}
                    >
                      {openTask.assigneeId == null ? 'Assign' : 'Reassign'}
                    </Button>
                  </div>
                </>
              )}
              {actionErrors[openTask.id] && <p className={styles.actionError} role="alert">{actionErrors[openTask.id]} Refresh the queue if the assignment has changed.</p>}
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
}
