import Link from 'next/link';
import styles from './page.module.css';

const claimSteps = [
  { number: '01', title: 'Share the details', detail: 'Tell us what happened' },
  { number: '02', title: 'We review your claim', detail: 'Your information is assessed' },
  { number: '03', title: 'Follow your progress', detail: 'Check in whenever you need' },
];

export default function HomePage() {
  return (
    <div className={styles.page}>
      <section className={styles.hero} aria-labelledby="welcome-title">
        <div className={styles.heroInner}>
          <div className={styles.heroContent}>
            <p className={styles.eyebrow}>
              <span className={styles.eyebrowMark} aria-hidden="true">
                <svg viewBox="0 0 20 20" fill="none">
                  <path d="M10 2.5 16 5v4.3c0 4-2.6 6.8-6 8.2-3.4-1.4-6-4.2-6-8.2V5l6-2.5Z" stroke="currentColor" strokeWidth="1.5" />
                  <path d="m7.3 9.8 1.8 1.8 3.7-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              Motor insurance claims, made clearer
            </p>
            <h1 className={styles.heroTitle} id="welcome-title">
              A simpler way to get back <span>on the road.</span>
            </h1>
            <p className={styles.heroSubtitle}>
              Start a motor claim online or check in on one you’ve already made. We’ll help you find your next step.
            </p>
            <div className={styles.heroActions}>
              <Link href="/file-claim" className={styles.heroButtonPrimary}>
                Start a claim
                <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                  <path d="M4 10h11m-4.5-4.5L15 10l-4.5 4.5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </Link>
              <Link href="/track" className={styles.heroButtonSecondary}>
                <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                  <circle cx="10" cy="10" r="7" stroke="currentColor" strokeWidth="1.5" />
                  <path d="M10 6v4l2.7 1.7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                Track a claim
              </Link>
            </div>
            <p className={styles.assurance}>
              <span aria-hidden="true">↳</span>
              No need to visit a branch to get started
            </p>
          </div>

          <aside className={styles.journey} aria-label="A guide to the claim journey">
            <div className={styles.journeyTop}>
              <div>
                <span className={styles.journeyKicker}>YOUR CLAIM JOURNEY</span>
                <h2 className={styles.journeyTitle}>One step at a time.</h2>
              </div>
              <div className={styles.journeyGlyph} aria-hidden="true">
                <svg viewBox="0 0 48 48" fill="none">
                  <path d="M8 31.5h32l-3-9.5a4 4 0 0 0-3.8-2.8H15a4 4 0 0 0-3.8 2.8L8 31.5Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
                  <path d="M11 31.5v4a2 2 0 0 0 2 2h2v-4m18 0v4a2 2 0 0 1-2 2h-2v-4M11 27h26" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                  <circle cx="16" cy="32" r="1.4" fill="currentColor" />
                  <circle cx="32" cy="32" r="1.4" fill="currentColor" />
                  <path d="M18 19.2v-3a6 6 0 0 1 12 0v3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
              </div>
            </div>

            <div className={styles.journeyRule} />
            <ol className={styles.steps}>
              {claimSteps.map((step, index) => (
                <li className={styles.step} key={step.number}>
                  <span className={`${styles.stepNumber} ${index === 0 ? styles.stepNumberActive : ''}`}>
                    {step.number}
                  </span>
                  <div className={styles.stepCopy}>
                    <h3>{step.title}</h3>
                    <p>{step.detail}</p>
                  </div>
                  {index === 0 && (
                    <span className={styles.stepStatus}>Begin here</span>
                  )}
                </li>
              ))}
            </ol>
            <div className={styles.journeyNote}>
              <span className={styles.noteDot} aria-hidden="true" />
              <p>Start when you’re ready. You can return to check your claim at any time.</p>
            </div>
          </aside>
        </div>
      </section>

      <section className={styles.trust} aria-label="Claims service at a glance">
        <div className={styles.trustIntro}>
          <span className={styles.trustRule} aria-hidden="true" />
          <p>Here to help you move forward</p>
        </div>
        <div className={styles.heroStats}>
          <div className={styles.heroStat}>
            <span className={styles.heroStatNumber}>5,000+</span>
            <span className={styles.heroStatLabel}>Claims Processed</span>
          </div>
          <span className={styles.heroStatDivider} aria-hidden="true" />
          <div className={styles.heroStat}>
            <span className={styles.heroStatNumber}>98%</span>
            <span className={styles.heroStatLabel}>Satisfaction Rate</span>
          </div>
          <span className={styles.heroStatDivider} aria-hidden="true" />
          <div className={styles.heroStat}>
            <span className={styles.heroStatNumber}>24/7</span>
            <span className={styles.heroStatLabel}>Support Available</span>
          </div>
        </div>
      </section>
    </div>
  );
}
