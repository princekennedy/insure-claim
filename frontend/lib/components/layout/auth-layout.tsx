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
            <div className={styles.logoContainer}>
              <span className={styles.logoText}>Britam</span>
              <span className={styles.logoTagline}>With you every step of the way</span>
            </div>
          </Link>
        </div>

        <div className={styles.content}>{children}</div>

        <div className={styles.footer}>
          <p>Britam Insurance PLC</p>
        </div>
      </div>
    </div>
  );
}
