import Link from 'next/link';
import styles from './page.module.css';

export default function HomePage() {
  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div className={styles.heroBackground}>
          <div className={styles.heroGradient} />
        </div>
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            <span>Trusted by thousands of policyholders</span>
          </div>
          <h1 className={styles.heroTitle}>
            Motor Insurance Claims
            <br />
            <span className={styles.heroTitleAccent}>Made Simple</span>
          </h1>
          <p className={styles.heroSubtitle}>
            File your claim in minutes, track progress in real-time, and get back on the road faster. Powered by Britam Insurance PLC.
          </p>
          <div className={styles.heroActions}>
            <Link href="/claims" className={styles.heroButtonPrimary}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              File a Claim
            </Link>
            <Link href="/track" className={styles.heroButtonSecondary}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              Track Existing Claim
            </Link>
          </div>
          <div className={styles.heroStats}>
            <div className={styles.heroStat}>
              <span className={styles.heroStatNumber}>5,000+</span>
              <span className={styles.heroStatLabel}>Claims Processed</span>
            </div>
            <div className={styles.heroStatDivider} />
            <div className={styles.heroStat}>
              <span className={styles.heroStatNumber}>98%</span>
              <span className={styles.heroStatLabel}>Satisfaction Rate</span>
            </div>
            <div className={styles.heroStatDivider} />
            <div className={styles.heroStat}>
              <span className={styles.heroStatNumber}>24/7</span>
              <span className={styles.heroStatLabel}>Support Available</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
