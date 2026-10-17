'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button, Textarea, Select, Card, Alert } from '@/lib/components/ui';
import { isAuthenticated } from '@/lib/services/auth';
import type { GarageFeedbackRequest } from '@/lib/types/garage';
import styles from './feedback.module.css';

type FeedbackDraft = Omit<GarageFeedbackRequest, 'recommendAgain'> & {
  recommendAgain: boolean | null;
};

export default function PublicFeedbackPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formData, setFormData] = useState<FeedbackDraft>({
    customerServiceRating: 0,
    repairQualityRating: 0,
    qualityRating: undefined as number | undefined,
    timelinessRating: undefined as number | undefined,
    priceFairnessRating: undefined as number | undefined,
    staffCourtesyRating: undefined as number | undefined,
    comments: '',
    recommendAgain: null,
  });

  useEffect(() => {
    if (isAuthenticated()) {
      router.push('/feedback');
    }
  }, [router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value ? parseFloat(value) : null,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.customerServiceRating) {
      setError('Please rate the customer service');
      return;
    }
    if (!formData.repairQualityRating) {
      setError('Please rate the repair quality');
      return;
    }
    if (formData.recommendAgain === null) {
      setError('Please tell us whether you would choose this garage again');
      return;
    }

    setError('');
    setLoading(true);
    setSuccess('Thank you for your feedback! Please sign in to submit your rating officially.');
    setLoading(false);
  };

  const ratingOptions = [1, 2, 3, 4, 5].map(num => ({
    value: num.toString(),
    label: num.toString(),
  }));

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1 className={styles.title}>Share Your Feedback</h1>
          <p className={styles.subtitle}>
            Help us improve by sharing your experience with our panel garages.
          </p>
        </div>

        <div className={styles.content}>
          <Card padding="lg" className={styles.feedbackCard}>
            <div className={styles.cardHeader}>
              <h2>Rate Your Experience</h2>
              <p>Your feedback helps us maintain high standards across our network</p>
            </div>

            {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
            {success && <Alert type="success" onDismiss={() => setSuccess('')}>{success}</Alert>}

            <form onSubmit={handleSubmit} className={styles.form}>
              <div className={styles.ratingSection}>
                <div className={styles.ratingHeader}>
                  <label className={styles.label}>Customer service *</label>
                  <span className={styles.required}>Required</span>
                </div>
                <p className={styles.ratingPrompt}>How were you treated by the garage team?</p>
                <div className={styles.ratingContainer}>
                  {ratingOptions.map((option) => (
                    <button
                      type="button"
                      key={option.value}
                      className={`${styles.ratingButton} ${parseFloat(option.value) <= formData.customerServiceRating ? styles.ratingActive : ''}`}
                      onClick={() => setFormData(prev => ({ ...prev, customerServiceRating: parseFloat(option.value) }))}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className={styles.ratingSection}>
                <div className={styles.ratingHeader}>
                  <label className={styles.label}>Repair quality *</label>
                  <span className={styles.required}>Required</span>
                </div>
                <p className={styles.ratingPrompt}>How satisfied are you with the completed repair?</p>
                <div className={styles.ratingContainer}>
                  {ratingOptions.map((option) => (
                    <button
                      type="button"
                      key={option.value}
                      className={`${styles.ratingButton} ${parseFloat(option.value) <= formData.repairQualityRating ? styles.ratingActive : ''}`}
                      onClick={() => setFormData(prev => ({ ...prev, repairQualityRating: parseFloat(option.value) }))}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className={styles.subRatings}>
                <div className={styles.subRating}>
                  <label className={styles.label}>Timeliness</label>
                  <Select
                    name="timelinessRating"
                    value={formData.timelinessRating?.toString() || ''}
                    onChange={handleChange}
                    options={ratingOptions}
                    placeholder="Select rating"
                  />
                </div>
                <div className={styles.subRating}>
                  <label className={styles.label}>Price Fairness</label>
                  <Select
                    name="priceFairnessRating"
                    value={formData.priceFairnessRating?.toString() || ''}
                    onChange={handleChange}
                    options={ratingOptions}
                    placeholder="Select rating"
                  />
                </div>
              </div>

              <fieldset className={styles.recommendField}>
                <legend className={styles.label}>Would you choose this garage again? *</legend>
                <div className={styles.recommendOptions}>
                  <label className={styles.recommendOption}>
                    <input
                      type="radio"
                      name="recommendAgain"
                      value="yes"
                      checked={formData.recommendAgain === true}
                      onChange={() => setFormData(prev => ({ ...prev, recommendAgain: true }))}
                    />
                    Yes, I would
                  </label>
                  <label className={styles.recommendOption}>
                    <input
                      type="radio"
                      name="recommendAgain"
                      value="no"
                      checked={formData.recommendAgain === false}
                      onChange={() => setFormData(prev => ({ ...prev, recommendAgain: false }))}
                    />
                    Not this time
                  </label>
                </div>
              </fieldset>

              <Textarea
                label="Comments (Optional)"
                name="comments"
                value={formData.comments || ''}
                onChange={handleChange}
                placeholder="Share any additional thoughts about your experience..."
                rows={4}
                hint="Your comments help us improve our service"
              />

              <div className={styles.formActions}>
                <Button type="submit" loading={loading} size="lg">
                  Submit Feedback
                </Button>
              </div>

              <p className={styles.formNote}>
                Your feedback is valuable and helps other customers make informed decisions.
              </p>
            </form>
          </Card>

          <div className={styles.infoSection}>
            <Card padding="md">
              <h3 className={styles.infoTitle}>Why Your Feedback Matters</h3>
              <p className={styles.infoText}>
                Your ratings help us maintain quality standards and help other customers choose the right garage for their repairs.
              </p>
            </Card>
            <Card padding="md">
              <div className={styles.infoItem}>
                <div className={styles.infoItemIcon}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="20" height="20">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                  </svg>
                </div>
                <div>
                  <h4 className={styles.infoItemTitle}>Anonymity Protected</h4>
                  <p className={styles.infoItemText}>Your personal information remains confidential</p>
                </div>
              </div>
              <div className={styles.infoItem}>
                <div className={styles.infoItemIcon}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="20" height="20">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  </svg>
                </div>
                <div>
                  <h4 className={styles.infoItemTitle}>Secure & Trusted</h4>
                  <p className={styles.infoItemText}>Your feedback is safely stored and reviewed</p>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
