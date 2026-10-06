'use client';

import { ReactNode } from 'react';
import Link from 'next/link';
import styles from './auth-layout.module.css';

interface AuthLayoutProps {
  children: ReactNode;
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className={styles.container}>
      <div className={styles.background}>
        <div className={styles.bgPattern} />
      </div>

      <div className={styles.card}>
        <div className={styles.header}>
          <Link href="/" className={styles.logo}>
            <svg viewBox="0 0 32 32" className={styles.logoIcon}>
              <rect x="2" y="2" width="28" height="28" rx="6" fill="#2563eb" />
              <path d="M10 22V10h4l6 6v6h-4l-6-6z" fill="white" />
            </svg>
            <span className={styles.logoText}>InsureClaim</span>
          </Link>
          <p className={styles.tagline}>Motor Insurance Claims Portal</p>
        </div>

        <div className={styles.content}>{children}</div>

        <div className={styles.footer}>
          <p>Britam Insurance PLC</p>
        </div>
      </div>
    </div>
  );
}
