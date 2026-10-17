'use client';

import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Button, Card, EmptyState, PageHeader, Textarea } from '@/lib/components/ui';
import { getMyClaims, getClaim } from '@/lib/services/claims';
import { getMyFeedback, submitFeedback } from '@/lib/services/garages';
import type { ClaimDetailResponse, ClaimSummaryResponse, GarageFeedbackResponse } from '@/lib/types';
import { formatDateTime } from '@/lib/types/claim';
import styles from './feedback.module.css';

const PAGE_SIZE = 10;
const RATING_LABELS = ['Choose a rating', 'Not good', 'Could be better', 'It was okay', 'Really good', 'Excellent'];

interface FeedbackClaim {
  summary: ClaimSummaryResponse;
  detail: ClaimDetailResponse;
}

function renderStars(rating: number): string {
  return '★'.repeat(rating) + '☆'.repeat(5 - rating);
}

export default function CustomerFeedbackPage() {
  const [feedback, setFeedback] = useState<GarageFeedbackResponse[]>([]);
  const [feedbackLoading, setFeedbackLoading] = useState(true);
  const [feedbackLoadingMore, setFeedbackLoadingMore] = useState(false);
  const [feedbackError, setFeedbackError] = useState('');
  const [feedbackPage, setFeedbackPage] = useState(0);
  const [hasMoreFeedback, setHasMoreFeedback] = useState(false);
  const [claims, setClaims] = useState<FeedbackClaim[]>([]);
  const [claimsLoading, setClaimsLoading] = useState(true);
  const [claimsError, setClaimsError] = useState('');
  const [selectedClaimId, setSelectedClaimId] = useState('');
  const [customerServiceRating, setCustomerServiceRating] = useState(0);
  const [repairQualityRating, setRepairQualityRating] = useState(0);
  const [comments, setComments] = useState('');
  const [recommendAgain, setRecommendAgain] = useState<boolean | null>(null);
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [retryKey, setRetryKey] = useState(0);
  const customerServiceRatingRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const repairQualityRatingRefs = useRef<Array<HTMLButtonElement | null>>([]);

  useEffect(() => {
    let cancelled = false;

    async function loadFeedback() {
      setFeedbackLoading(true);
      setFeedbackError('');
      try {
        const response = await getMyFeedback(0, PAGE_SIZE);
        if (cancelled) return;
        setFeedback(response.content);
        setFeedbackPage(0);
        setHasMoreFeedback(!response.last);
      } catch (error) {
        if (!cancelled) {
          setFeedbackError(error instanceof Error ? error.message : 'We couldn’t load your feedback.');
        }
      } finally {
        if (!cancelled) setFeedbackLoading(false);
      }
    }

    async function loadFeedbackClaims() {
      setClaimsLoading(true);
      setClaimsError('');
      try {
        const response = await getMyClaims(0, 50);
        const candidates = response.content.filter((claim) => claim.garageName);
        const details = await Promise.all(
          candidates.map(async (summary) => {
            try {
              const detail = await getClaim(summary.id);
              return detail.canSubmitFeedback && detail.repair
                ? { summary, detail }
                : null;
            } catch {
              return null;
            }
          }),
        );
        if (!cancelled) setClaims(details.filter((claim): claim is FeedbackClaim => claim !== null));
      } catch (error) {
        if (!cancelled) {
          setClaimsError(error instanceof Error ? error.message : 'We couldn’t check your completed repairs.');
        }
      } finally {
        if (!cancelled) setClaimsLoading(false);
      }
    }

    void loadFeedback();
    void loadFeedbackClaims();
    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  const availableClaims = useMemo(
    () => claims.filter((claim) => !feedback.some((item) => item.claimId === claim.summary.id)),
    [claims, feedback],
  );
  const selectedClaim =
    availableClaims.find((claim) => String(claim.summary.id) === selectedClaimId) ??
    availableClaims[0];

  async function loadMoreFeedback() {
    const nextPage = feedbackPage + 1;
    setFeedbackLoadingMore(true);
    setFeedbackError('');
    try {
      const response = await getMyFeedback(nextPage, PAGE_SIZE);
      setFeedback((current) => [...current, ...response.content]);
      setFeedbackPage(nextPage);
      setHasMoreFeedback(!response.last);
    } catch (error) {
      setFeedbackError(error instanceof Error ? error.message : 'We couldn’t load more feedback.');
    } finally {
      setFeedbackLoadingMore(false);
    }
  }

  function handleRatingKeyDown(
    event: React.KeyboardEvent<HTMLButtonElement>,
    value: number,
    setValue: React.Dispatch<React.SetStateAction<number>>,
    refs: React.MutableRefObject<Array<HTMLButtonElement | null>>,
  ) {
    let nextRating = value;
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') nextRating = Math.min(5, value + 1);
    else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') nextRating = Math.max(1, value - 1);
    else return;

    event.preventDefault();
    setValue(nextRating);
    refs.current[nextRating - 1]?.focus();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError('');
    setSuccessMessage('');

    if (!selectedClaim) {
      setFormError('Choose a completed repair before sending your feedback.');
      return;
    }
    if (customerServiceRating < 1 || customerServiceRating > 5) {
      setFormError('Please rate the customer service to continue.');
      return;
    }
    if (repairQualityRating < 1 || repairQualityRating > 5) {
      setFormError('Please rate the repair quality to continue.');
      return;
    }
    if (recommendAgain === null) {
      setFormError('Please tell us whether you would choose this garage again.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await submitFeedback(selectedClaim.summary.id, {
        customerServiceRating,
        repairQualityRating,
        comments: comments.trim() || undefined,
        recommendAgain,
      });
      setFeedback((current) => [created, ...current.filter((item) => item.id !== created.id)]);
      setCustomerServiceRating(0);
      setRepairQualityRating(0);
      setComments('');
      setRecommendAgain(null);
      setSuccessMessage('Thank you — your feedback has been submitted.');
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Your feedback could not be submitted. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      <PageHeader
        title="Your experience matters"
        description="A few honest words help us make every claim and repair journey better."
      />

      <div className={styles.content}>
        <section className={styles.composerSection} aria-labelledby="feedback-form-title">
          <div className={styles.sectionHeading}>
            <span className={styles.eyebrow}>A moment to reflect</span>
            <h2 id="feedback-form-title">How did it go?</h2>
            <p>Your feedback is connected to a repair you’ve completed.</p>
          </div>

          <Card className={styles.formCard} padding="none">
            <form className={styles.form} onSubmit={handleSubmit}>
              {claimsLoading ? (
                <p className={styles.loadingNote} role="status">Checking your completed repairs…</p>
              ) : claimsError ? (
                <div className={styles.inlineError} role="alert">
                  <span>{claimsError}</span>
                  <Button type="button" size="sm" variant="secondary" onClick={() => setRetryKey((key) => key + 1)}>
                    Try again
                  </Button>
                </div>
              ) : availableClaims.length > 0 ? (
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="feedback-claim">Repair experience</label>
                  <select
                    className={styles.select}
                    id="feedback-claim"
                    value={selectedClaim ? String(selectedClaim.summary.id) : ''}
                    onChange={(event) => setSelectedClaimId(event.target.value)}
                    required
                  >
                    {availableClaims.map(({ summary, detail }) => (
                      <option key={summary.id} value={summary.id}>
                        {summary.claimNumber} · {detail.repair?.garageName ?? summary.garageName}
                      </option>
                    ))}
                  </select>
                  {selectedClaim && (
                    <span className={styles.fieldHint}>
                      {selectedClaim.detail.repair?.garageName ?? selectedClaim.summary.garageName}
                      {' · '}Claim {selectedClaim.summary.claimNumber}
                    </span>
                  )}
                </div>
              ) : (
                <div className={styles.noRepairs} role="status">
                  <span className={styles.noRepairsMark} aria-hidden="true">✓</span>
                  <div>
                    <strong>{claims.length > 0 ? 'You’ve shared feedback on every repair currently available to review' : 'No completed repairs to review yet'}</strong>
                    <p>
                      {claims.length > 0
                        ? 'If you’ve just finished a repair, it may take a little while to become available here.'
                        : 'Once a repair is complete, you can tell us how the experience felt.'}
                    </p>
                  </div>
                </div>
              )}

              <fieldset className={styles.ratingField} disabled={!selectedClaim || submitting}>
                <legend className={styles.label}>Customer service <span aria-hidden="true">*</span></legend>
                <p className={styles.ratingPrompt}>How were you treated by the garage team?</p>
                <div className={styles.ratingControl} role="radiogroup" aria-label="Customer service rating, from 1 to 5 stars" aria-required="true">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <button
                      key={value}
                      ref={(element) => { customerServiceRatingRefs.current[value - 1] = element; }}
                      className={`${styles.starButton} ${value <= customerServiceRating ? styles.starSelected : ''}`}
                      type="button"
                      role="radio"
                      aria-checked={customerServiceRating === value}
                      aria-label={`${value} ${value === 1 ? 'star' : 'stars'} — ${RATING_LABELS[value]}`}
                      tabIndex={customerServiceRating === value || (customerServiceRating === 0 && value === 1) ? 0 : -1}
                      onClick={() => setCustomerServiceRating(value)}
                      onKeyDown={(event) => handleRatingKeyDown(event, value, setCustomerServiceRating, customerServiceRatingRefs)}
                    >
                      <span aria-hidden="true">{value <= customerServiceRating ? '★' : '☆'}</span>
                    </button>
                  ))}
                </div>
                <span className={styles.ratingCaption} aria-live="polite">
                  {RATING_LABELS[customerServiceRating]}{customerServiceRating > 0 ? ` · ${customerServiceRating} out of 5` : ''}
                </span>
              </fieldset>

              <fieldset className={styles.ratingField} disabled={!selectedClaim || submitting}>
                <legend className={styles.label}>Repair quality <span aria-hidden="true">*</span></legend>
                <p className={styles.ratingPrompt}>How satisfied are you with the completed repair?</p>
                <div className={styles.ratingControl} role="radiogroup" aria-label="Repair quality rating, from 1 to 5 stars" aria-required="true">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <button
                      key={value}
                      ref={(element) => { repairQualityRatingRefs.current[value - 1] = element; }}
                      className={`${styles.starButton} ${value <= repairQualityRating ? styles.starSelected : ''}`}
                      type="button"
                      role="radio"
                      aria-checked={repairQualityRating === value}
                      aria-label={`${value} ${value === 1 ? 'star' : 'stars'} — ${RATING_LABELS[value]}`}
                      tabIndex={repairQualityRating === value || (repairQualityRating === 0 && value === 1) ? 0 : -1}
                      onClick={() => setRepairQualityRating(value)}
                      onKeyDown={(event) => handleRatingKeyDown(event, value, setRepairQualityRating, repairQualityRatingRefs)}
                    >
                      <span aria-hidden="true">{value <= repairQualityRating ? '★' : '☆'}</span>
                    </button>
                  ))}
                </div>
                <span className={styles.ratingCaption} aria-live="polite">
                  {RATING_LABELS[repairQualityRating]}{repairQualityRating > 0 ? ` · ${repairQualityRating} out of 5` : ''}
                </span>
              </fieldset>

              <fieldset className={styles.recommendField} disabled={!selectedClaim || submitting}>
                <legend className={styles.label}>Would you choose this garage again? <span aria-hidden="true">*</span></legend>
                <div className={styles.recommendOptions}>
                  {[
                    { label: 'Yes, I would', value: true },
                    { label: 'Not this time', value: false },
                  ].map((option) => (
                    <button
                      key={option.label}
                      className={`${styles.recommendButton} ${recommendAgain === option.value ? styles.recommendSelected : ''}`}
                      type="button"
                      aria-pressed={recommendAgain === option.value}
                      onClick={() => setRecommendAgain(option.value)}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </fieldset>

              <Textarea
                label="Anything you’d like us to know?"
                value={comments}
                onChange={(event) => setComments(event.target.value)}
                placeholder="Tell us what went well, or what we could make easier next time."
                maxLength={1000}
                hint={`${comments.length}/1,000 characters · Your comment is optional.`}
                disabled={!selectedClaim || submitting}
                className={styles.commentBox}
              />

              {formError && <p className={styles.formError} role="alert">{formError}</p>}
              {successMessage && <p className={styles.successMessage} role="status">{successMessage}</p>}
              <div className={styles.formFooter}>
                <span className={styles.privacyNote}>Your feedback helps us improve the claims experience.</span>
                <Button type="submit" size="lg" loading={submitting} disabled={!selectedClaim || claimsLoading}>
                  Send feedback
                </Button>
              </div>
            </form>
          </Card>
        </section>

        <section className={styles.historySection} aria-labelledby="history-title">
          <div className={styles.historyHeading}>
            <div>
              <span className={styles.eyebrow}>Your voice, on record</span>
              <h2 id="history-title">Feedback you’ve shared</h2>
            </div>
            {!feedbackLoading && feedback.length > 0 && (
              <span className={styles.count}>{feedback.length} {feedback.length === 1 ? 'entry' : 'entries'}</span>
            )}
          </div>

          {feedbackError && (
            <div className={styles.historyError} role="alert">
              <p>{feedbackError}</p>
              <Button type="button" variant="secondary" size="sm" onClick={() => setRetryKey((key) => key + 1)}>
                Reload feedback
              </Button>
            </div>
          )}

          {feedbackLoading ? (
            <div className={styles.historyLoading} role="status">Loading your feedback…</div>
          ) : feedback.length > 0 ? (
            <div className={styles.historyList}>
              {feedback.map((item) => (
                <article className={styles.historyEntry} key={item.id}>
                  <div className={styles.entryTopline}>
                    <div className={styles.entryIdentity}>
                      <span className={styles.entryGarage}>{item.garageName}</span>
                      <span className={styles.entryClaim}>Claim {item.claimNumber}</span>
                    </div>
                    <time className={styles.entryDate} dateTime={item.createdAt}>
                      {formatDateTime(item.createdAt)}
                    </time>
                  </div>
                  <div className={styles.entryRating}>
                    <span className={styles.entryStars} aria-label={`${item.overallRating} out of 5 stars`}>
                      {renderStars(item.overallRating)}
                    </span>
                    <span>Overall · {item.overallRating}/5</span>
                  </div>
                  {(item.customerServiceRating !== null || item.repairQualityRating !== null) && (
                    <div className={styles.entryBreakdown}>
                      {item.customerServiceRating !== null && <span>Customer service · {item.customerServiceRating}/5</span>}
                      {item.repairQualityRating !== null && <span>Repair quality · {item.repairQualityRating}/5</span>}
                    </div>
                  )}
                  {item.comments && <p className={styles.entryComment}>{item.comments}</p>}
                  {item.recommendAgain !== null && (
                    <span className={styles.recommendNote}>
                      {item.recommendAgain ? 'Would choose again' : 'Would not choose again'}
                    </span>
                  )}
                </article>
              ))}
            </div>
          ) : !feedbackError ? (
            <EmptyState
              title="Your story starts here"
              description="Feedback you send after a completed repair will be kept here for you."
              className={styles.emptyHistory}
            />
          ) : null}

          {hasMoreFeedback && (
            <div className={styles.loadMore}>
              <Button type="button" variant="secondary" loading={feedbackLoadingMore} onClick={loadMoreFeedback}>
                Load older feedback
              </Button>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
