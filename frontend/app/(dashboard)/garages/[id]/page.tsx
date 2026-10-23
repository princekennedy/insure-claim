'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  PageHeader,
  PageLoader,
} from '@/lib/components/ui';
import { getGarage, getGarageFeedback } from '@/lib/services/garages';
import { formatDateTime } from '@/lib/types/claim';
import type { GarageFeedbackResponse, GarageResponse } from '@/lib/types';
import styles from './garage-detail.module.css';

const PAGE_SIZE = 20;

function renderStars(rating: number): string {
  const roundedRating = Math.max(0, Math.min(5, Math.round(rating)));
  return '★'.repeat(roundedRating) + '☆'.repeat(5 - roundedRating);
}

export default function GarageDetailPage() {
  const { id } = useParams<{ id: string }>();
  const garageId = Number(id);
  const [garage, setGarage] = useState<GarageResponse | null>(null);
  const [feedback, setFeedback] = useState<GarageFeedbackResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');

  const loadFeedback = useCallback(async (pageNum: number) => {
    setError('');
    if (pageNum > 0) setLoadingMore(true);

    try {
      const response = await getGarageFeedback(garageId, pageNum, PAGE_SIZE);
      setFeedback((current) =>
        pageNum === 0 ? response.content : [...current, ...response.content],
      );
      setPage(pageNum);
      setHasMore(!response.last);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load garage ratings.');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [garageId]);

  useEffect(() => {
    let cancelled = false;

    async function loadGarageDetails() {
      try {
        const [garageDetails, feedbackPage] = await Promise.all([
          getGarage(garageId),
          getGarageFeedback(garageId, 0, PAGE_SIZE),
        ]);
        if (cancelled) return;
        setGarage(garageDetails);
        setFeedback(feedbackPage.content);
        setHasMore(!feedbackPage.last);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Unable to load garage details.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    if (Number.isSafeInteger(garageId) && garageId > 0) void loadGarageDetails();

    return () => {
      cancelled = true;
    };
  }, [garageId]);

  if (!Number.isSafeInteger(garageId) || garageId <= 0) {
    return (
      <div className={styles.page}>
        <Alert type="error">Invalid garage.</Alert>
      </div>
    );
  }

  if (loading && !garage) {
    return <PageLoader message="Loading garage ratings..." />;
  }

  if (!garage) {
    return (
      <div className={styles.page}>
        <Alert type="error">
          <div>
            <p>{error || 'Unable to load this garage.'}</p>
            <Button variant="secondary" size="sm" onClick={() => window.location.reload()}>
              Try again
            </Button>
          </div>
        </Alert>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <Link href="/garages" className={styles.backLink}>← All garages</Link>

      <PageHeader
        title={garage.name}
        description={`${garage.code} · ${garage.address}, ${garage.city}`}
      />

      <section className={styles.summary} aria-label="Garage rating summary">
        <div>
          <span className={styles.summaryLabel}>Customer rating</span>
          <strong className={styles.summaryValue}>
            {garage.ratingCount > 0 ? `${garage.ratingAverage.toFixed(1)} / 5` : 'No ratings'}
          </strong>
        </div>
        <div>
          <span className={styles.summaryLabel}>Ratings received</span>
          <strong className={styles.summaryValue}>{garage.ratingCount}</strong>
        </div>
        <div>
          <span className={styles.summaryLabel}>Complaints</span>
          <strong className={styles.summaryValue}>{garage.complaintCount}</strong>
        </div>
      </section>

      <div className={styles.sectionHeading}>
        <h2>Customer ratings</h2>
        <Badge variant={garage.performanceStatus === 'GOOD' ? 'success' : garage.performanceStatus === 'WATCH' ? 'warning' : 'danger'}>
          {garage.performanceLabel}
        </Badge>
      </div>

      {error && (
        <Alert type="error">
          <div>
            <p>{error}</p>
            <Button variant="secondary" size="sm" onClick={() => void loadFeedback(page)}>
              Try again
            </Button>
          </div>
        </Alert>
      )}

      {feedback.length === 0 && !error ? (
        <EmptyState
          title="No customer ratings yet"
          description="Ratings submitted for completed work at this garage will appear here."
        />
      ) : feedback.length > 0 ? (
        <div className={styles.feedbackList}>
          {feedback.map((item) => (
            <Card key={item.id}>
              <CardHeader className={styles.feedbackHeader}>
                <div>
                  <CardTitle>Claim {item.claimNumber}</CardTitle>
                  <p className={styles.meta}>{formatDateTime(item.createdAt)}</p>
                </div>
                <Badge variant={item.isComplaint ? 'danger' : item.overallRating >= 4 ? 'success' : item.overallRating >= 3 ? 'warning' : 'secondary'}>
                  {item.overallRating}/5
                </Badge>
              </CardHeader>
              <CardContent>
                <p className={styles.stars} aria-label={`${item.overallRating} out of 5 stars`}>
                  {renderStars(item.overallRating)}
                </p>
                <div className={styles.breakdown}>
                  {item.customerServiceRating != null && (
                    <span className={styles.pill}>Customer service {item.customerServiceRating}</span>
                  )}
                  {item.repairQualityRating != null && (
                    <span className={styles.pill}>Repair quality {item.repairQualityRating}</span>
                  )}
                  {item.qualityRating != null && (
                    <span className={styles.pill}>Quality {item.qualityRating}</span>
                  )}
                  {item.timelinessRating != null && (
                    <span className={styles.pill}>Timeliness {item.timelinessRating}</span>
                  )}
                  {item.priceFairnessRating != null && (
                    <span className={styles.pill}>Price {item.priceFairnessRating}</span>
                  )}
                  {item.staffCourtesyRating != null && (
                    <span className={styles.pill}>Courtesy {item.staffCourtesyRating}</span>
                  )}
                </div>
                {item.comments && item.commentVisible && (
                  <p className={styles.comment}>{item.comments}</p>
                )}
                {item.recommendAgain != null && (
                  <p className={styles.meta}>
                    {item.recommendAgain
                      ? 'Customer would choose this garage again'
                      : 'Customer would not choose this garage again'}
                  </p>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}

      {hasMore && (
        <div className={styles.loadMore}>
          <Button
            variant="secondary"
            loading={loadingMore}
            onClick={() => void loadFeedback(page + 1)}
          >
            Load more ratings
          </Button>
        </div>
      )}
    </div>
  );
}
