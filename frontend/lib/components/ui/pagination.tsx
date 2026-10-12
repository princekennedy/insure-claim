'use client';

import { Button } from './button';
import styles from './pagination.module.css';

interface PaginationProps {
  page: number;
  totalPages: number;
  label: string;
  loading?: boolean;
  onPageChange: (page: number) => void;
}

export function Pagination({
  page,
  totalPages,
  label,
  loading = false,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav className={styles.pagination} aria-label={label}>
      <span className={styles.pageCount}>
        Page {page + 1} of {totalPages}
      </span>
      <div className={styles.paginationActions}>
        <Button
          type="button"
          variant="secondary"
          disabled={page <= 0 || loading}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={page >= totalPages - 1 || loading}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </Button>
      </div>
    </nav>
  );
}
