'use client';

import { useState, useEffect } from 'react';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  EmptyState,
  PageLoader,
} from '@/lib/components/ui';
import { getPolicies } from '@/lib/services/policies';
import { formatCurrency, formatDate } from '@/lib/types/claim';
import { POLICY_STATUS_LABELS } from '@/lib/types/policy';
import type { PolicyResponse, PolicyStatus } from '@/lib/types';
import styles from './policies.module.css';

function statusVariant(status: string): 'success' | 'warning' | 'danger' | 'secondary' {
  if (status === 'ACTIVE') return 'success';
  if (status === 'EXPIRED') return 'warning';
  if (status === 'CANCELLED') return 'danger';
  return 'secondary';
}

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getPolicies()
      .then((data) => setPolicies(data))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load policies'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <PageLoader message="Loading policies..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Policies"
        description="Your insurance policies and coverage"
      />

      {error && <p className={styles.error}>{error}</p>}

      {policies.length === 0 ? (
        <EmptyState
          title="No policies found"
          description="Your insurance policies will appear here once issued."
        />
      ) : (
        <div className={styles.list}>
          {policies.map((policy) => (
            <Card key={policy.id} className={styles.policyCard}>
              <CardHeader className={styles.policyHeader}>
                <div>
                  <CardTitle>{policy.policyNumber}</CardTitle>
                  <p className={styles.insurer}>{policy.insurerName}</p>
                </div>
                <Badge variant={statusVariant(policy.status)}>
                  {POLICY_STATUS_LABELS[policy.status as PolicyStatus] || policy.status}
                </Badge>
              </CardHeader>
              <CardContent>
                <p className={styles.product}>{policy.productCode.replace(/_/g, ' ')}</p>
                <div className={styles.vehicle}>
                  <span className={styles.reg}>{policy.vehicle.registrationNumber}</span>
                  <span>{policy.vehicle.make} {policy.vehicle.model} ({policy.vehicle.year})</span>
                </div>

                <div className={styles.amountsGrid}>
                  <div className={styles.amountItem}>
                    <span className={styles.amountLabel}>Sum Insured</span>
                    <span className={styles.amountValue}>{formatCurrency(policy.sumInsured)}</span>
                  </div>
                  <div className={styles.amountItem}>
                    <span className={styles.amountLabel}>Annual Premium</span>
                    <span className={styles.amountValue}>{formatCurrency(policy.premiumAmount)}</span>
                  </div>
                  <div className={styles.amountItem}>
                    <span className={styles.amountLabel}>Excess</span>
                    <span className={styles.amountValue}>{formatCurrency(policy.excessAmount)}</span>
                  </div>
                </div>

                <div className={styles.dates}>
                  <span>Valid {formatDate(policy.startDate)} → {formatDate(policy.endDate)}</span>
                  {policy.isCurrentlyValid && (
                    <Badge variant="info" size="sm">Current</Badge>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}