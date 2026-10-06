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
  Button,
} from '@/lib/components/ui';
import { getGarages } from '@/lib/services/garages';
import type { GarageResponse, GaragePerformanceStatus } from '@/lib/types';
import styles from './garages.module.css';

function statusVariant(status: GaragePerformanceStatus): 'success' | 'warning' | 'danger' | 'secondary' {
  if (status === 'GOOD') return 'success';
  if (status === 'WATCH') return 'warning';
  if (status === 'UNDERPERFORMING' || status === 'SUSPENDED') return 'danger';
  return 'secondary';
}

export default function GaragesPage() {
  const [garages, setGarages] = useState<GarageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const loadGarages = useCallback(async (pageNum = 0, city?: string) => {
    setLoading(true);
    try {
      const response = await getGarages(pageNum, 12, city, query || undefined);
      setGarages((prev) =>
        pageNum === 0 ? response.content : [...prev, ...response.content],
      );
      setPage(pageNum);
      setHasMore(!response.last);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load garages');
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    getGarages(0, 12)
      .then((response) => {
        setGarages(response.content);
        setHasMore(!response.last);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load garages'))
      .finally(() => setLoading(false));
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadGarages(0);
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="Panel Garages"
        description="Find a garage for your repair work"
      />

      <form onSubmit={handleSearch} className={styles.searchRow}>
        <input
          className={styles.searchInput}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name or city..."
        />
        <Button type="submit">Search</Button>
      </form>

      {error && <p className={styles.error}>{error}</p>}

      {!loading && garages.length === 0 ? (
        <EmptyState
          title="No garages available"
          description="No panel garages match your search."
        />
      ) : (
        <>
          <div className={styles.grid}>
            {garages.map((garage) => (
              <Card key={garage.id} className={styles.garageCard}>
                <CardHeader className={styles.garageHeader}>
                  <div>
                    <CardTitle>{garage.name}</CardTitle>
                    <p className={styles.city}>{garage.city}</p>
                  </div>
                  <Badge variant={statusVariant(garage.performanceStatus)}>
                    {garage.performanceLabel}
                  </Badge>
                </CardHeader>
                <CardContent>
                  <p className={styles.address}>{garage.address}</p>
                  <p className={styles.phone}>{garage.contactPhone}</p>
                  <div className={styles.stats}>
                    <div className={styles.stat}>
                      <span className={styles.statValue}>
                        {garage.ratingCount > 0 ? garage.ratingAverage.toFixed(1) : '—'}
                      </span>
                      <span className={styles.statLabel}>Rating</span>
                    </div>
                    <div className={styles.stat}>
                      <span className={styles.statValue}>{garage.jobsCompleted}</span>
                      <span className={styles.statLabel}>Jobs</span>
                    </div>
                    <div className={styles.stat}>
                      <span className={styles.statValue}>{garage.complaintCount}</span>
                      <span className={styles.statLabel}>Complaints</span>
                    </div>
                  </div>
                  <div className={styles.foot}>
                    {garage.isPanelGarage && (
                      <Badge variant="info" size="sm">Panel</Badge>
                    )}
                    <Badge variant={garage.acceptingWork ? 'success' : 'secondary'} size="sm">
                      {garage.acceptingWork ? 'Accepting work' : 'Not accepting'}
                    </Badge>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {hasMore && (
            <div className={styles.loadMore}>
              <Button onClick={() => loadGarages(page + 1)} loading={loading}>
                Load More
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}