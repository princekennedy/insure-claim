'use client';

import { useState, useEffect, useCallback } from 'react';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  EmptyState,
  PageLoader,
  Button,
} from '@/lib/components/ui';
import { getMyFeedback } from '@/lib/services/garages';
import { formatDateTime } from '@/lib/types/claim';
import type { GarageFeedbackResponse } from '@/lib/types';
import styles from './feedback.module.css';

function renderStars(rating: number): string {
  return '★'.repeat(Math.max(1, Math.round(rating))) + '☆'.repeat(5 - Math.max(1, Math.round(rating)));
}

export default function FeedbackPage() {
  const [items, setItems] = useState<GarageFeedbackResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const load = useCallback(async (pageNum = 0) => {
    try {
      const response = await getMyFeedback(pageNum, 10);
      setItems((prev) =>
        pageNum === 0 ? response.content : [...prev, ...response.content],
      );
      setPage(pageNum);
      setHasMore(!response.last);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load feedback');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getMyFeedback(0, 10)
      .then((response) => {
        setItems(response.content);
        setHasMore(!response.last);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load feedback'))
      .finally(() => setLoading(false));
  }, []);

  if (loading && items.length === 0) {
    return <PageLoader message="Loading feedback..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="My Feedback"
        description="Ratings you've submitted for garage repairs"
      />

      {error && <p className={styles.error}>{error}</p>}

      {items.length === 0 ? (
        <EmptyState
          title="No feedback submitted"
          description="Your ratings for completed repairs will appear here."
        />
      ) : (
        <div className={styles.list}>
          {items.map((item) => (
            <Card key={item.id} className={styles.feedbackCard}>
              <CardHeader className={styles.feedbackHeader}>
                <div>
                  <CardTitle>{item.garageName}</CardTitle>
                  <p className={styles.meta}>
                    Claim {item.claimNumber} · {formatDateTime(item.createdAt)}
                  </p>
                </div>
                <Badge
                  variant={
                    item.isComplaint ? 'danger' : item.overallRating >= 4 ? 'success' : item.overallRating >= 3 ? 'warning' : 'secondary'
                  }
                >
                  {item.overallRating}/5
                </Badge>
              </CardHeader>
              <CardContent>
                <p className={styles.stars}>{renderStars(item.overallRating)}</p>
                <div className={styles.breakdown}>
                  {item.qualityRating !== null && item.qualityRating !== undefined && (
                    <span className={styles.pill}>Quality {item.qualityRating}</span>
                  )}
                  {item.timelinessRating !== null && item.timelinessRating !== undefined && (
                    <span className={styles.pill}>Timeliness {item.timelinessRating}</span>
                  )}
                  {item.priceFairnessRating !== null && item.priceFairnessRating !== undefined && (
                    <span className={styles.pill}>Price {item.priceFairnessRating}</span>
                  )}
                  {item.staffCourtesyRating !== null && item.staffCourtesyRating !== undefined && (
                    <span className={styles.pill}>Courtesy {item.staffCourtesyRating}</span>
                  )}
                </div>
                {item.comments && item.commentVisible && (
                  <p className={styles.comment}>{item.comments}</p>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {hasMore && (
        <div className={styles.loadMore}>
          <Button onClick={() => load(page + 1)} loading={loading}>
            Load More
          </Button>
        </div>
      )}
    </div>
  );
}