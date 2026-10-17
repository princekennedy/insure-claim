'use client';

import { FormEvent, useEffect, useState } from 'react';
import {
  Badge,
  Button,
  EmptyState,
  PageHeader,
  PageLoader,
  ProgressBar,
} from '@/lib/components/ui';
import { getMyClaims } from '@/lib/services/claims';
import {
  CLAIM_STATUS_LABELS,
  CLAIM_STATUS_PROGRESS,
  formatCurrency,
  formatDate,
  INCIDENT_TYPE_LABELS,
} from '@/lib/types/claim';
import type { ClaimStatus, ClaimSummaryResponse } from '@/lib/types/claim';
import styles from './claims.module.css';

const PAGE_SIZE = 20;

type CustomerClaimStatus = Exclude<
  ClaimStatus,
  'GARAGE_ASSIGNED' | 'IN_REPAIR' | 'READY_FOR_PICKUP'
>;

const CUSTOMER_CLAIM_STATUSES: CustomerClaimStatus[] = [
  'DRAFT',
  'SUBMITTED',
  'KYC_PENDING',
  'UNDER_REVIEW',
  'FRAUD_CHECK',
  'APPROVED',
  'REJECTED',
  'SETTLED',
  'CLOSED',
  'WITHDRAWN',
];

function customerClaimStatus(status: ClaimStatus): CustomerClaimStatus {
  if (
    status === 'GARAGE_ASSIGNED' ||
    status === 'IN_REPAIR' ||
    status === 'READY_FOR_PICKUP'
  ) {
    return 'APPROVED';
  }

  return status;
}

function statusVariant(status: ClaimStatus): 'success' | 'danger' | 'info' {
  if (status === 'SETTLED' || status === 'CLOSED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'danger';
  return 'info';
}

export default function CustomerClaimsPage() {
  const [claims, setClaims] = useState<ClaimSummaryResponse[]>([]);
  const [searchInput, setSearchInput] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<CustomerClaimStatus | ''>('');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadClaims() {
      setLoading(true);
      setError('');

      try {
        const response = await getMyClaims(page, PAGE_SIZE, {
          query: query || undefined,
          status: status === 'APPROVED'
            ? ['APPROVED', 'GARAGE_ASSIGNED', 'IN_REPAIR', 'READY_FOR_PICKUP']
            : status || undefined,
        });

        if (cancelled) return;
        setClaims((current) =>
          page === 0 ? response.content : [...current, ...response.content],
        );
        setTotalElements(response.totalElements);
        setHasMore(!response.last);
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : 'Unable to load claims.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadClaims();
    return () => {
      cancelled = true;
    };
  }, [page, query, retryKey, status]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setQuery(searchInput.trim());
  }

  function handleStatusChange(value: string) {
    setPage(0);
    setStatus(value as CustomerClaimStatus | '');
  }

  if (loading && claims.length === 0 && !error) {
    return <PageLoader message="Loading claims..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Claims"
        description="Track the status and progress of your insurance claims."
      />

      <section className={styles.overview} aria-label="Claims overview">
        <div>
          <span className={styles.overviewLabel}>Claims found</span>
          <strong className={styles.overviewValue}>{totalElements}</strong>
        </div>
        <span className={styles.overviewNote}>
          {status ? CLAIM_STATUS_LABELS[status] : 'Across all statuses'}
        </span>
      </section>

      <div className={styles.toolbar}>
        <form className={styles.searchForm} onSubmit={handleSearch} role="search">
          <label className={styles.srOnly} htmlFor="claim-search">Search claims</label>
          <input
            id="claim-search"
            className={styles.searchInput}
            type="search"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Claim number or vehicle"
          />
          <Button type="submit" size="sm">Search</Button>
        </form>

        <div className={styles.statusFilter}>
          <label htmlFor="claim-status">Status</label>
          <select
            id="claim-status"
            value={status}
            onChange={(event) => handleStatusChange(event.target.value)}
          >
            <option value="">All statuses</option>
            {CUSTOMER_CLAIM_STATUSES.map((value) => (
              <option key={value} value={value}>{CLAIM_STATUS_LABELS[value]}</option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className={styles.errorState} role="alert">
          <p>{error}</p>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setRetryKey((key) => key + 1)}
          >
            Try again
          </Button>
        </div>
      )}

      {!error && claims.length === 0 ? (
        <EmptyState
          title={query || status ? 'No matching claims' : 'No claims yet'}
          description={
            query || status
              ? 'Try changing your search or status filter.'
              : 'Claims you submit will appear here with their latest status and progress.'
          }
        />
      ) : claims.length > 0 ? (
        <>
          <div className={styles.listHeader} aria-hidden="true">
            <span>Claim</span>
            <span>Vehicle</span>
            <span>Submitted</span>
            <span>Progress</span>
            <span>Status</span>
          </div>
          <div className={styles.claimList}>
            {claims.map((claim) => {
              const displayStatus = customerClaimStatus(claim.status);
              const progress = CLAIM_STATUS_PROGRESS[displayStatus];

              return (
                <article className={styles.claimRow} key={claim.id}>
                  <div className={styles.claimIdentity}>
                    <strong>{claim.claimNumber}</strong>
                    <span>{INCIDENT_TYPE_LABELS[claim.incidentType]}</span>
                  </div>
                  <div className={styles.vehicle}>
                    <strong>{claim.vehicleRegistration}</strong>
                    <span>{claim.vehicleLabel}</span>
                  </div>
                  <time className={styles.date} dateTime={claim.submittedAt}>
                    {formatDate(claim.submittedAt)}
                  </time>
                  <div className={styles.progress}>
                    <ProgressBar value={progress} size="sm" />
                    <span>{progress}%</span>
                  </div>
                  <div className={styles.statusCell}>
                    <Badge variant={statusVariant(displayStatus)}>
                      {CLAIM_STATUS_LABELS[displayStatus]}
                    </Badge>
                    {claim.approvedAmount !== null && (
                      <span className={styles.amount}>
                        {formatCurrency(claim.approvedAmount)} approved
                      </span>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
          <div className={styles.listFooter}>
            <span>Showing {claims.length} of {totalElements} claims</span>
            {hasMore && (
              <Button
                variant="secondary"
                loading={loading}
                onClick={() => setPage((current) => current + 1)}
              >
                Load more
              </Button>
            )}
          </div>
        </>
      ) : null}
    </div>
  );
}