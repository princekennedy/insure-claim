'use client';

import { useState, useEffect, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Button, Input, Select, Textarea, Card, Alert, Badge } from '@/lib/components/ui';
import { isAuthenticated } from '@/lib/services/auth';
import { INCIDENT_TYPE_LABELS } from '@/lib/types/claim';
import styles from './claims.module.css';

export default function PublicFileClaimPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formData, setFormData] = useState({
    description: '',
    incidentType: '',
    incidentDate: new Date().toISOString().split('T')[0],
    incidentLocation: '',
    estimatedAmount: '',
    reportedByPolice: false,
    thirdPartyInvolved: false,
    policyNumber: '',
    vehicleRegistration: '',
  });

  useEffect(() => {
    if (isAuthenticated()) {
      router.push('/claims');
    }
  }, [router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    setSuccess('Thank you! Please sign in to complete your claim submission.');
    setLoading(false);
  };

  const incidentTypes = Object.entries(INCIDENT_TYPE_LABELS).map(([value, label]) => ({
    value,
    label,
  }));

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <div className={styles.header}>
          <div className={styles.headerContent}>
            <h1 className={styles.title}>File a Claim</h1>
            <p className={styles.subtitle}>Report your insurance claim in just a few steps. Have your policy number and vehicle details ready.</p>
          </div>
        </div>

        <div className={styles.content}>
          <Card padding="lg" className={styles.claimCard}>
            <div className={styles.formHeader}>
              <h2>Claim Details</h2>
              <p>All fields marked with * are required</p>
            </div>

            {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
            {success && <Alert type="success" onDismiss={() => setSuccess('')}>{success}</Alert>}

            <form onSubmit={handleSubmit} className={styles.form}>
              <div className={styles.formGrid}>
                <Select
                  label="Incident Type *"
                  name="incidentType"
                  value={formData.incidentType}
                  onChange={handleChange}
                  options={incidentTypes}
                  placeholder="Select incident type"
                  required
                  hint="Select the type of incident"
                />
                <Input
                  label="Incident Date *"
                  type="date"
                  name="incidentDate"
                  value={formData.incidentDate}
                  onChange={handleChange}
                  required
                />
              </div>

              <Input
                label="Incident Location"
                type="text"
                name="incidentLocation"
                value={formData.incidentLocation}
                onChange={handleChange}
                placeholder="e.g., Mombasa Road, Nairobi"
                hint="Where did the incident occur?"
              />

              <Textarea
                label="Description *"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Describe what happened in detail..."
                rows={5}
                required
                hint="Include how the incident happened and any damage observed"
              />

              <div className={styles.formGrid}>
                <Input
                  label="Estimated Amount"
                  type="number"
                  name="estimatedAmount"
                  value={formData.estimatedAmount}
                  onChange={handleChange}
                  placeholder="e.g., 50000"
                  hint="Approximate repair cost (optional)"
                />
                <Input
                  label="Policy Number"
                  type="text"
                  name="policyNumber"
                  value={formData.policyNumber}
                  onChange={handleChange}
                  placeholder="e.g., POL-2024-001234"
                  hint="From your insurance documents"
                />
              </div>

              <Input
                label="Vehicle Registration"
                type="text"
                name="vehicleRegistration"
                value={formData.vehicleRegistration}
                onChange={handleChange}
                placeholder="e.g., KCB 123J"
                hint="Your vehicle registration number"
              />

              <div className={styles.checkboxes}>
                <label className={styles.checkbox}>
                  <input type="checkbox" name="reportedByPolice" checked={formData.reportedByPolice} onChange={handleChange} />
                  <span>Reported to police</span>
                </label>
                <label className={styles.checkbox}>
                  <input type="checkbox" name="thirdPartyInvolved" checked={formData.thirdPartyInvolved} onChange={handleChange} />
                  <span>Third party involved</span>
                </label>
              </div>

              <div className={styles.formActions}>
                <Button type="submit" loading={loading} size="lg" fullWidth>
                  Submit Claim Request
                </Button>
              </div>

              <p className={styles.formNote}>
                By submitting, you agree to our terms. Our team will review your claim and contact you within 24 hours.
              </p>
            </form>
          </Card>

          <div className={styles.infoSection}>
            <Card padding="md">
              <h3 className={styles.infoTitle}>What You'll Need</h3>
              <ul className={styles.infoList}>
                <li>Vehicle registration number</li>
                <li>Policy number (if available)</li>
                <li>Photos of the damage</li>
                <li>Police report (if applicable)</li>
                <li>Your contact information</li>
              </ul>
            </Card>
            <Card padding="md">
              <h3 className={styles.infoTitle}>Processing Time</h3>
              <p className={styles.infoText}>
                Most claims are reviewed within 24-48 hours. You'll receive updates via SMS and email.
              </p>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
