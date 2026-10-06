'use client';

import { ReactNode, useEffect } from 'react';
import styles from './alert.module.css';

interface AlertProps {
  type?: 'info' | 'success' | 'warning' | 'error';
  children: ReactNode;
  dismissible?: boolean;
  onDismiss?: () => void;
  className?: string;
}

export function Alert({
  type = 'info',
  children,
  dismissible = false,
  onDismiss,
  className = '',
}: AlertProps) {
  const typeClass = styles[type];

  return (
    <div className={`${styles.alert} ${typeClass} ${className}`} role="alert">
      <div className={styles.content}>{children}</div>
      {dismissible && onDismiss && (
        <button
          type="button"
          className={styles.dismiss}
          onClick={onDismiss}
          aria-label="Dismiss"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" className={styles.dismissIcon}>
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
              clipRule="evenodd"
            />
          </svg>
        </button>
      )}
    </div>
  );
}

// Toast-like alert that auto-dismisses
interface ToastAlertProps extends Omit<AlertProps, 'onDismiss'> {
  duration?: number;
}

export function ToastAlert({
  type = 'info',
  children,
  dismissible = true,
  duration = 5000,
  className = '',
}: ToastAlertProps) {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    if (duration > 0 && !dismissible) {
      const timer = setTimeout(() => setIsVisible(false), duration);
      return () => clearTimeout(timer);
    }
  }, [duration, dismissible]);

  if (!isVisible) return null;

  return (
    <Alert
      type={type}
      dismissible={dismissible}
      onDismiss={() => setIsVisible(false)}
      className={className}
    >
      {children}
    </Alert>
  );
}

import { useState } from 'react';
