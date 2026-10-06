'use client';

import { useState, useEffect } from 'react';
import { Button, Input, Card, Alert, Badge, ProgressBar, PageLoader } from '@/lib/components/ui';
import { getPublicTracking, createTrackingLink } from '@/lib/services/claims';
import { formatDateTime, CLAIM_STATUS_LABELS } from '@/lib/types/claim';
import styles from './track.module.css';

export default function PublicTrackPage() {
  const [trackingToken, setTrackingToken] = useState('');
  const [claim, setClaim] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleTrack = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!trackingToken.trim()) return;

    setError('');
    setLoading(true);
    setClaim(null);

    try {
      const data = await getPublicTracking(trackingToken.trim());
      setClaim(data);
      setSuccess('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid tracking token');
      setClaim(null);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateLink = async () => {
    // In production, this would require authentication
    setSuccess('Please sign in to generate a tracking link for your claim.');
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1 className={styles.title}>Track Your Claim</h1>
          <p className={styles.subtitle}>
            Enter your tracking token to see your claim status. No sign-in required.
          </p>
        </div>

        <div className={styles.content}>
          <Card padding="lg" className={styles.trackCard}>
            <form onSubmit={handleTrack} className={styles.form}>
              <div className={styles.inputGroup}>
                <Input
                  label="Tracking Token"
                  type="text"
                  value={trackingToken}
                  onChange={(e) => setTrackingToken(e.target.value)}
                  placeholder="Enter your tracking token"
                  hint="Found in your claim confirmation email or SMS"
                />
              </div>

              <div className={styles.formActions}>
                <Button type="submit" loading={loading} size="lg">
                  Track Claim
                </Button>
              </div>
            </form>

            {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
            {success && <Alert type="info" onDismiss={() => setSuccess('')}>{success}</Alert>}

            {submitted && (
              <div className={styles.linkSection}>
                <p className={styles.linkText}>Don't have a token?</p>
                <Button onClick={handleCreateLink} variant="secondary" size="sm">
                  Request a Tracking Link
                </Button>
              </div>
            )}
          </Card>

          {claim && (
            <Card padding="lg" className={styles.claimCard}>
              <div className={styles.claimHeader}>
                <div className={styles.claimInfo}>
                  <Badge variant="info" className={styles.claimBadge}>
                    {claim.statusLabel || 'In Progress'}
                  </Badge>
                  <h2 className={styles.claimNumber}>{claim.claimNumber}</h2>
                </div>
                <div className={styles.claimMeta}>
                  <span>{claim.claim.incidentType?.replace('_', ' ') || 'Incident'}</span>
                  <span>·</span>
                  <span>{formatDateTime(claim.incidentDate || new Date().toISOString())}</span>
                </div>
              </div>

              <div className={styles.progressSection}>
                <div className={styles.progressHeader}>
                  <h3>Claim Progress</h3>
                  <span>{claim.progressPercent || 0}% Complete</span>
                </div>
                <ProgressBar value={claim.progressPercent || 0} size="lg" />
              </div>

              <div className={styles.timelineSection}>
                <h3 className={styles.timelineTitle}>Status Timeline</h3>
                <div className={styles.timeline}>
                  {claim.timeline?.map((entry: any, index: number) => (
                    <div
                      key={index}
                      className={`${styles.timelineItem} ${entry.isCurrent ? styles.current : ''}`}
                    >
                      <div className={styles.timelineDot} />
                      <div className={styles.timelineContent}>
                        <span className={styles.timelineStatus}>{entry.label}</span>
                        <span className={styles.timelineDate}>{formatDateTime(entry.occurredAt)}</span>
                      </div>
                    </div>
                  ))}
                  {(!claim.timeline || claim.timeline.length === 0) && (
                    <p className={styles.noTimeline}>No timeline entries yet</p>
                  )}
                </div>
              </div>

              <div className={styles.claimDetails}>
                <h3 className={styles.detailsTitle}>Claim Details</h3>
                <div className={styles.detailsGrid}>
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Vehicle</span>
                    <span className={styles.detailValue}>{claim.vehicleRegistration || 'N/A'}</span>
                  </div>
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Incident Type</span>
                    <span className={styles.detailValue}>
                      {claim.incidentType?.replace('_', ' ') || 'N/A'}
                    </span>
                  </div>
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Reported</span>
                    <span className={styles.detailValue}>
                      {formatDateTime(claim.incidentDate || new Date().toISOString())}
                    </span>
                  </div>
                </div>
              </div>
            </Card>
          )}

          {(!claim && !loading) && (
            <div className={styles.placeholder}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className={styles.placeholderIcon}>
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <h3>Track Your Claim</h3>
              <p>Enter your tracking token above to see your claim status and timeline.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
