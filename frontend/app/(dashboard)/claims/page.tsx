'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { Alert, PageHeader, Card, Badge, ProgressBar, EmptyState, PageLoader, Button } from '@/lib/components/ui';
import { getClaims } from '@/lib/services/claims';
import { formatCurrency, formatDateTime } from '@/lib/types/claim';
import type { ClaimSummaryResponse, ClaimStatus } from '@/lib/types/claim';
import styles from './claims.module.css';

function getStatusVariant(status: ClaimStatus): 'success' | 'danger' | 'info' {
  if (status === 'SETTLED' || status === 'CLOSED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'danger';
  return 'info';
}

export default function ClaimsPage() {
  const [claims, setClaims] = useState<ClaimSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState('');

  const loadClaims = useCallback(async (pageNum: number) => {
    try {
      const response = await getClaims(pageNum, 20);
      setClaims((prev) => {
        if (pageNum === 0) return response.content;
        const existing = new Set(prev.map((c) => c.id));
        return [...prev, ...response.content.filter((c) => !existing.has(c.id))];
      });
      setHasMore(!response.last);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Unable to load customer claims.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    getClaims(0, 20)
      .then((response) => {
        if (cancelled) return;
        setClaims(response.content);
        setHasMore(!response.last);
      })
      .catch((error) => {
        if (cancelled) return;
        setError(error instanceof Error ? error.message : 'Unable to load customer claims.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    setError('');
    setLoading(true);
    void loadClaims(nextPage);
  };

  const retryLoading = () => {
    setError('');
    setLoading(true);
    void loadClaims(page);
  };

  if (loading && claims.length === 0) {
    return <PageLoader message="Loading claims..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Claims"
        description="Review and manage claims submitted by customers."
      />

      {error && (
        <Alert type="error">
          <div>
            <p>{error}</p>
            <Button variant="secondary" size="sm" onClick={retryLoading}>
              Try again
            </Button>
          </div>
        </Alert>
      )}

      {claims.length === 0 && !error ? (
        <EmptyState
          title="No customer claims yet"
          description="Claims submitted by customers will appear here."
        />
      ) : claims.length > 0 ? (
        <div className={styles.claimsGrid}>
          {claims.map((claim) => (
            <Link key={claim.id} href={`/claims/${claim.id}`} className={styles.claimLink}>
              <Card hover className={styles.claimCard}>
                <div className={styles.claimHeader}>
                  <div className={styles.claimInfo}>
                    <span className={styles.claimNumber}>{claim.claimNumber}</span>
                    <span className={styles.claimType}>
                      {claim.incidentType.length > 0
                        ? claim.incidentType.replace('_', ' ').toLowerCase()
                        : 'Claim'}
                    </span>
                  </div>
                  <Badge variant={getStatusVariant(claim.status)}>
                    {claim.statusLabel}
                  </Badge>
                </div>

                <div className={styles.claimMeta}>
                  <span>{claim.vehicleRegistration}</span>
                  <span>·</span>
                  <span>{formatDateTime(claim.submittedAt)}</span>
                </div>

                <div className={styles.claimAmounts}>
                  {claim.estimatedAmount !== null && claim.estimatedAmount !== undefined && (
                    <span className={styles.amount}>
                      Estimated: {formatCurrency(claim.estimatedAmount)}
                    </span>
                  )}
                  {claim.approvedAmount !== null && claim.approvedAmount !== undefined && (
                    <span className={styles.amount}>
                      Approved: {formatCurrency(claim.approvedAmount)}
                    </span>
                  )}
                </div>

                <div className={styles.claimProgress}>
                  <ProgressBar value={claim.progressPercent} size="sm" showPercentage />
                </div>

                {claim.garageName && (
                  <div className={styles.garageInfo}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                    {claim.garageName}
                  </div>
                )}
              </Card>
            </Link>
          ))}
        </div>
      ) : null}

      {hasMore && claims.length > 0 && (
        <div className={styles.loadMore}>
          <Button onClick={loadMore} loading={loading}>
            Load More
          </Button>
        </div>
      )}
    </div>
  );
}