'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { PageHeader, Card, Badge, ProgressBar, EmptyState, PageLoader, Button } from '@/lib/components/ui';
import { getMyClaims } from '@/lib/services/claims';
import { formatCurrency, formatDateTime } from '@/lib/types/claim';
import type { ClaimSummaryResponse, ClaimStatus } from '@/lib/types/claim';
import styles from './claims.module.css';

function getStatusVariant(status: ClaimStatus): 'success' | 'danger' | 'info' {
  if (status === 'SETTLED' || status === 'CLOSED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'danger';
  return 'info';
}

export default function ClaimsPage() {
  const router = useRouter();
  const [claims, setClaims] = useState<ClaimSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const loadClaims = useCallback(async (pageNum: number) => {
    try {
      const response = await getMyClaims(pageNum, 20);
      setClaims((prev) => {
        if (pageNum === 0) return response.content;
        const existing = new Set(prev.map((c) => c.id));
        return [...prev, ...response.content.filter((c) => !existing.has(c.id))];
      });
      setHasMore(!response.last);
    } catch (error) {
      console.error('Failed to load claims:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getMyClaims(0, 20)
      .then((response) => {
        setClaims(response.content);
        setHasMore(!response.last);
      })
      .catch((error) => console.error('Failed to load claims:', error))
      .finally(() => setLoading(false));
  }, []);

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    setLoading(true);
    loadClaims(nextPage);
  };

  if (loading && claims.length === 0) {
    return <PageLoader message="Loading claims..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Claims"
        description="View and manage your insurance claims"
        actions={
          <Link href="/claims/new" className={styles.newButton}>
            <Button>File New Claim</Button>
          </Link>
        }
      />

      {claims.length === 0 ? (
        <EmptyState
          title="No claims yet"
          description="You haven't filed any claims yet. File your first claim to get started."
          action={{
            label: 'File a Claim',
            onClick: () => router.push('/claims/new'),
          }}
        />
      ) : (
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
      )}

      {hasMore && (
        <div className={styles.loadMore}>
          <Button onClick={loadMore} loading={loading}>
            Load More
          </Button>
        </div>
      )}
    </div>
  );
}