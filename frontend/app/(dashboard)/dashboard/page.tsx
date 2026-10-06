'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  ProgressBar,
  Avatar,
  EmptyState,
  PageLoader,
} from '@/lib/components/ui';
import { getMeSummary, getCurrentUser } from '@/lib/services/auth';
import { getClaims } from '@/lib/services/claims';
import { formatCurrency, formatDate } from '@/lib/types/claim';
import type { CustomerSummary, UserResponse, ClaimSummaryResponse } from '@/lib/types';
import styles from './dashboard.module.css';

export default function DashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<CustomerSummary | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);
  const [recentClaims, setRecentClaims] = useState<ClaimSummaryResponse[]>([]);

  useEffect(() => {
    async function fetchData() {
      try {
        const [summaryData, userData, claimsData] = await Promise.all([
          getMeSummary(),
          getCurrentUser(),
          getClaims(0, 5),
        ]);
        setSummary(summaryData);
        setUser(userData);
        setRecentClaims(claimsData.content);
      } catch (error) {
        console.error('Failed to load dashboard:', error);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, []);

  if (loading) {
    return <PageLoader message="Loading dashboard..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Dashboard"
        description="Overview of your insurance claims"
      />

      <div className={styles.statsGrid}>
        <StatCard
          title="Total Claims"
          value={summary?.totalClaims ?? 0}
          icon={ClaimsIcon}
          color="blue"
        />
        <StatCard
          title="Open Claims"
          value={summary?.openClaims ?? 0}
          icon={OpenIcon}
          color="amber"
        />
        <StatCard
          title="Settled"
          value={summary?.settledClaims ?? 0}
          icon={CheckIcon}
          color="green"
        />
        <StatCard
          title="Rejected"
          value={summary?.rejectedClaims ?? 0}
          icon={CloseIcon}
          color="red"
        />
      </div>

      <div className={styles.grid}>
        <Card>
          <CardHeader>
            <CardTitle>Financial Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <div className={styles.financialGrid}>
              <div className={styles.financialItem}>
                <span className={styles.financialLabel}>Total Claimed</span>
                <span className={styles.financialValue}>
                  {formatCurrency(summary?.totalClaimedAmount ?? 0)}
                </span>
              </div>
              <div className={styles.financialItem}>
                <span className={styles.financialLabel}>Total Settled</span>
                <span className={styles.financialValue}>
                  {formatCurrency(summary?.totalSettledAmount ?? 0)}
                </span>
              </div>
              <div className={styles.financialItem}>
                <span className={styles.financialLabel}>KYC Verified</span>
                <Badge variant={summary?.kycVerified ? 'success' : 'warning'}>
                  {summary?.kycVerified ? 'Yes' : 'No'}
                </Badge>
              </div>
              <div className={styles.financialItem}>
                <span className={styles.financialLabel}>Active Policy</span>
                <span className={styles.financialValue}>
                  {summary?.activePolicy?.policyNumber ?? '—'}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Claims</CardTitle>
          </CardHeader>
          <CardContent>
            {recentClaims.length === 0 ? (
              <EmptyState
                title="No claims yet"
                description="You haven't filed any claims. Start by filing your first claim."
                action={{
                  label: 'File a Claim',
                  onClick: () => router.push('/claims/new'),
                }}
              />
            ) : (
              <div className={styles.claimsList}>
                {recentClaims.map((claim) => (
                  <Link
                    key={claim.id}
                    href={`/claims/${claim.id}`}
                    className={styles.claimRow}
                  >
                    <div className={styles.claimInfo}>
                      <span className={styles.claimNumber}>{claim.claimNumber}</span>
                      <span className={styles.claimMeta}>
                        {claim.vehicleLabel} · {formatDate(claim.submittedAt)}
                      </span>
                    </div>
                    <div className={styles.claimStatus}>
                      <Badge
                        variant={
                          claim.status === 'SETTLED' || claim.status === 'CLOSED'
                            ? 'success'
                            : claim.status === 'REJECTED' || claim.status === 'WITHDRAWN'
                              ? 'danger'
                              : 'info'
                        }
                      >
                        {claim.statusLabel}
                      </Badge>
                      <ProgressBar
                        value={claim.progressPercent}
                        size="sm"
                        showPercentage
                      />
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>My Profile</CardTitle>
          </CardHeader>
          <CardContent>
            <div className={styles.profileCard}>
              <Avatar name={user?.fullName || 'User'} size="lg" />
              <div className={styles.profileInfo}>
                <span className={styles.profileName}>{user?.fullName ?? '—'}</span>
                <span className={styles.profileEmail}>{user?.email ?? '—'}</span>
              </div>
              <Badge variant="secondary">{user?.role ?? '—'}</Badge>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatCard({
  title,
  value,
  icon: Icon,
  color,
}: {
  title: string;
  value: number;
  icon: React.ElementType;
  color: 'blue' | 'amber' | 'green' | 'red';
}) {
  return (
    <Card
      className={`${styles.statCard} ${styles[`statCard${color.charAt(0).toUpperCase() + color.slice(1)}`]}`}
    >
      <div className={styles.statIcon}>
        <Icon />
      </div>
      <div className={styles.statContent}>
        <span className={styles.statValue}>{value}</span>
        <span className={styles.statTitle}>{title}</span>
      </div>
    </Card>
  );
}

function ClaimsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-6 h-6">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
    </svg>
  );
}

function OpenIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-6 h-6">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-6 h-6">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
      <polyline points="22 4 12 14.01 9 11.01" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-6 h-6">
      <circle cx="12" cy="12" r="10" />
      <line x1="15" y1="9" x2="9" y2="15" />
      <line x1="9" y1="9" x2="15" y2="15" />
    </svg>
  );
}