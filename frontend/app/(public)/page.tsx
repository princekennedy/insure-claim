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
          <div className={styles.heroLeft}>
            <h1 className={styles.heroTitle}>
              Motor Insurance Claims
              <br />
              <span className={styles.heroTitleAccent}>Made Simple</span>
            </h1>
            <p className={styles.heroSubtitle}>
              File your claim in minutes, track progress in real-time, and get back on the road faster. Powered by Britam Insurance PLC.
            </p>
            <Link href="/auth/login" className={styles.heroCtaButton}>
              File a Claim Now
            </Link>
            <div className={styles.heroStats}>
              <div className={styles.heroStat}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24" className={styles.heroStatIcon}>
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                  <polyline points="10 9 9 9 8 9" />
                </svg>
                <span className={styles.heroStatNumber}>5,000+</span>
                <span className={styles.heroStatLabel}>Claims Processed</span>
              </div>
              <div className={styles.heroStatDivider} />
              <div className={styles.heroStat}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24" className={styles.heroStatIcon}>
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span className={styles.heroStatNumber}>98%</span>
                <span className={styles.heroStatLabel}>Satisfaction Rate</span>
              </div>
              <div className={styles.heroStatDivider} />
              <div className={styles.heroStat}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24" className={styles.heroStatIcon}>
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                </svg>
                <span className={styles.heroStatNumber}>24/7</span>
                <span className={styles.heroStatLabel}>Support Available</span>
              </div>
            </div>
            <div className={styles.heroTestimonial}>
              <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" className={styles.testimonialQuote}>
                <path d="M14.017 21v-7.391c0-5.704 3.731-9.57 8.983-10.609l.995 2.151c-2.432.917-3.995 3.638-3.995 5.849h4v10h-9.983zm-14.017 0v-7.391c0-5.704 3.748-9.57 9-10.609l.996 2.151c-2.433.917-3.996 3.638-3.996 5.849h3.983v10h-9.983z" />
              </svg>
              <p className={styles.testimonialText}>&ldquo;Britam made my claim process so smooth. I was back on the road in just 3 days!&rdquo;</p>
              <span className={styles.testimonialAuthor}>— John M., Nairobi</span>
            </div>
          </div>
          <div className={styles.heroRight}>
            <div className={styles.heroVisual}>
              <div className={styles.visualCard}>
                <div className={styles.visualHeader}>
                  <div className={styles.visualStep}>
                    <span className={styles.visualStepNumber}>1</span>
                    <span className={styles.visualStepText}>Submit Claim</span>
                  </div>
                  <div className={styles.visualStep}>
                    <span className={styles.visualStepNumber}>2</span>
                    <span className={styles.visualStepText}>Review</span>
                  </div>
                  <div className={styles.visualStep}>
                    <span className={styles.visualStepNumber}>3</span>
                    <span className={styles.visualStepText}>Garage Assigned</span>
                  </div>
                  <div className={styles.visualStep}>
                    <span className={styles.visualStepNumber}>4</span>
                    <span className={styles.visualStepText}>Vehicle Fixed</span>
                  </div>
                  <div className={styles.visualStep}>
                    <span className={styles.visualStepNumber}>5</span>
                    <span className={styles.visualStepText}>Service Rated</span>
                  </div>
                </div>
                <div className={styles.visualBody}>
                  <div className={styles.visualProgress}>
                    <div className={styles.visualProgressBar}>
                      <div className={styles.visualProgressFill} />
                    </div>
                    <span className={styles.visualProgressText}>Claim in Progress</span>
                  </div>
                  <div className={styles.visualDetails}>
                    <div className={styles.visualDetail}>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                        <circle cx="12" cy="12" r="10" />
                        <polyline points="12 6 12 12 16 14" />
                      </svg>
                      <span>Submitted: Today</span>
                    </div>
                    <div className={styles.visualDetail}>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                        <polyline points="22 4 12 14.01 9 11.01" />
                      </svg>
                      <span>Documents Verified</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
