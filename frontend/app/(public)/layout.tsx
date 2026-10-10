import { ReactNode } from 'react';
import Link from 'next/link';
import styles from './layout.module.css';

export default function PublicLayout({ children }: { children: ReactNode }) {
  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link href="/" className={styles.logo}>
            <div className={styles.logoContainer}>
              <span className={styles.logoText}>Britam</span>
              <span className={styles.logoTagline}>With you every step of the way</span>
            </div>
          </Link>
          <nav className={styles.nav}>
            <Link href="/login" className={styles.navLink}>Track Claim</Link>
            <Link href="/login" className={styles.navLink}>Feedback</Link>
            <Link href="/login" className={styles.navButton}>Sign In</Link>
          </nav>
        </div>
      </header>

      <main className={styles.main}>{children}</main>

      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <div className={styles.footerBrand}>
            <svg viewBox="0 0 32 32" className={styles.footerLogo}>
              <rect x="2" y="2" width="28" height="28" rx="6" fill="#2563eb" />
              <path d="M10 22V10h4l6 6v6h-4l-6-6z" fill="white" />
            </svg>
            <span>InsureClaim Portal</span>
          </div>
          <div className={styles.footerLinks}>
            <Link href="/login">File a Claim</Link>
            <Link href="/login">Track Claim</Link>
            <Link href="/login">Feedback</Link>
            <Link href="/login">Sign In</Link>
          </div>
          <p className={styles.footerCopy}>
            Britam Insurance PLC - Motor Insurance Claims Platform
          </p>
        </div>
      </footer>
    </div>
  );
}
