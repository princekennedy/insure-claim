'use client';

import { ReactNode } from 'react';
import styles from './badge.module.css';

interface BadgeProps {
  children: ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'secondary';
  size?: 'sm' | 'md';
  className?: string;
}

export function Badge({
  children,
  variant = 'default',
  size = 'md',
  className = '',
}: BadgeProps) {
  const sizeClass = styles[`size-${size}`];

  return (
    <span className={`${styles.badge} ${styles[variant]} ${sizeClass} ${className}`}>
      {children}
    </span>
  );
}
