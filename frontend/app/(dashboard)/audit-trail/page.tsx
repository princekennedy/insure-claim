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
import { getAuditTrail } from '@/lib/services/admin';
import type { AuditLogResponse, PageResponse } from '@/lib/types';
import styles from '../management.module.css';

const PAGE_SIZE = 20;

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
}

export default function AuditTrailPage() {
  const [result, setResult] = useState<PageResponse<AuditLogResponse> | null>(null);
  const [query, setQuery] = useState('');
  const [action, setAction] = useState('');
  const [success, setSuccess] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [appliedFilters, setAppliedFilters] = useState({
    query: '',
    action: '',
    success: '',
    from: '',
    to: '',
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAuditTrail = useCallback(async (
    page: number,
    filters: typeof appliedFilters,
  ) => {
    setLoading(true);
    setError('');
    try {
      setResult(await getAuditTrail(page, PAGE_SIZE, {
        query: filters.query || undefined,
        action: filters.action || undefined,
        success: filters.success === '' ? undefined : filters.success === 'true',
        from: filters.from || undefined,
        to: filters.to || undefined,
      }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load the audit trail.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAuditTrail(0, appliedFilters);
  }, [appliedFilters, loadAuditTrail]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (from && to && from > to) {
      setError('The start date must be on or before the end date.');
      return;
    }
    setAppliedFilters({ query: query.trim(), action: action.trim(), success, from, to });
  }

  function clearFilters() {
    setQuery('');
    setAction('');
    setSuccess('');
    setFrom('');
    setTo('');
    setAppliedFilters({ query: '', action: '', success: '', from: '', to: '' });
  }

  return (
    <section className={styles.page}>
      <PageHeader
        title="Audit Trail"
        description="Search recorded activity by actor, action, outcome, and date."
      />

      <form className={styles.filterBar} onSubmit={handleSearch} aria-label="Filter audit trail">
        <div className={styles.filterField}>
          <label className={styles.filterLabel} htmlFor="audit-query">Search activity</label>
          <input
            className={styles.filterInput}
            id="audit-query"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Actor, action, entity, or path"
          />
        </div>
        <div className={styles.filterFieldNarrow}>
          <label className={styles.filterLabel} htmlFor="audit-action">Action code</label>
          <input
            className={styles.filterInput}
            id="audit-action"
            value={action}
            onChange={(event) => setAction(event.target.value)}
            placeholder="e.g. CLAIM_CREATE"
          />
        </div>
        <div className={styles.filterFieldNarrow}>
          <label className={styles.filterLabel} htmlFor="audit-success">Outcome</label>
          <select
            className={styles.filterSelect}
            id="audit-success"
            value={success}
            onChange={(event) => setSuccess(event.target.value)}
          >
            <option value="">All outcomes</option>
            <option value="true">Successful</option>
            <option value="false">Failed</option>
          </select>
        </div>
        <div className={styles.filterFieldNarrow}>
          <label className={styles.filterLabel} htmlFor="audit-from">From</label>
          <input
            className={styles.filterInput}
            id="audit-from"
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
        </div>
        <div className={styles.filterFieldNarrow}>
          <label className={styles.filterLabel} htmlFor="audit-to">To</label>
          <input
            className={styles.filterInput}
            id="audit-to"
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
        </div>
        <div className={styles.filterActions}>
          <Button type="submit" loading={loading}>Apply</Button>
          <Button type="button" variant="secondary" onClick={clearFilters}>Clear</Button>
        </div>
      </form>

      {error && (
        <>
          <Alert type="error">{error}</Alert>
          {!error.includes('start date') && (
            <div className={styles.errorActions}>
              <Button
                type="button"
                variant="secondary"
                onClick={() => void loadAuditTrail(result?.page ?? 0, appliedFilters)}
                loading={loading}
              >
                Try again
              </Button>
            </div>
          )}
        </>
      )}

      {loading && !result ? (
        <PageLoader message="Loading audit activity..." />
      ) : result ? (
        <>
          <p className={styles.resultLine}>
            {result.totalElements} {result.totalElements === 1 ? 'activity record' : 'activity records'}
            {Object.values(appliedFilters).some(Boolean) ? ' match these filters' : ' recorded'}
          </p>
          {result.empty ? (
            <EmptyState
              title="No activity found"
              description="No audit records match the selected search and filters."
            />
          ) : (
            <Card className={styles.tableCard} padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Audit activity records</caption>
                  <thead>
                    <tr>
                      <th scope="col">When</th>
                      <th scope="col">Actor</th>
                      <th scope="col">Action</th>
                      <th scope="col">Activity</th>
                      <th scope="col">Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.content.map((entry) => (
                      <tr key={entry.id}>
                        <td>{formatDate(entry.createdAt)}</td>
                        <td>
                          <span className={styles.primaryCell}>{entry.actorEmail || 'Unknown actor'}</span>
                          {entry.actorRole && <span className={styles.secondaryText}>{entry.actorRole}</span>}
                        </td>
                        <td className={styles.primaryCell}>{entry.action}</td>
                        <td className={styles.auditDescription}>
                          {entry.description}
                          <span className={styles.secondaryText}>
                            {entry.requestMethod} {entry.requestPath}
                            {entry.entityType && ` · ${entry.entityType}`}
                            {entry.entityId && ` #${entry.entityId}`}
                          </span>
                          {entry.detail && (
                            <details className={styles.auditDetails}>
                              <summary>View details</summary>
                              <p>{entry.detail}</p>
                            </details>
                          )}
                        </td>
                        <td>
                          <Badge variant={entry.success ? 'success' : 'danger'} size="sm">
                            {entry.success ? 'Successful' : 'Failed'}
                          </Badge>
                          <span className={styles.statusCode}>HTTP {entry.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
          {result.totalPages > 1 && (
            <nav className={styles.pagination} aria-label="Audit trail pages">
              <span className={styles.pageCount}>
                Page {result.page + 1} of {result.totalPages}
              </span>
              <div className={styles.paginationActions}>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={result.first || loading}
                  onClick={() => void loadAuditTrail(result.page - 1, appliedFilters)}
                >
                  Previous
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={result.last || loading}
                  onClick={() => void loadAuditTrail(result.page + 1, appliedFilters)}
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
