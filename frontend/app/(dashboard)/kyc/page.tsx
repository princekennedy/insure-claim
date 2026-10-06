'use client';

import { useState, useEffect } from 'react';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Input,
  Select,
  Badge,
  Alert,
  PageLoader,
} from '@/lib/components/ui';
import { getCurrentKyc, submitKyc } from '@/lib/services/kyc';
import { KYC_DOCUMENT_TYPE_LABELS } from '@/lib/types/kyc';
import { formatDateTime } from '@/lib/types/claim';
import type { KycVerificationResponse, KycDocumentType } from '@/lib/types';
import styles from './kyc.module.css';

export default function KycPage() {
  const [current, setCurrent] = useState<KycVerificationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [documentType, setDocumentType] = useState<KycDocumentType>('NIC');
  const [documentNumber, setDocumentNumber] = useState('');
  const [fullNameOnDocument, setFullNameOnDocument] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [frontImage, setFrontImage] = useState<File | null>(null);
  const [backImage, setBackImage] = useState<File | null>(null);
  const [selfie, setSelfie] = useState<File | null>(null);

  useEffect(() => {
    getCurrentKyc()
      .then((data) => setCurrent(data))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load KYC status'))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!frontImage) {
      setError('Front image of your ID document is required');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      const result = await submitKyc(
        {
          documentType,
          documentNumber,
          fullNameOnDocument,
          dateOfBirth: dateOfBirth || undefined,
        },
        { frontImage, backImage: backImage || undefined, selfie: selfie || undefined },
      );
      setCurrent(result);
      setSuccess('KYC verification submitted for review');
      setDocumentNumber('');
      setFullNameOnDocument('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit KYC');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <PageLoader message="Loading KYC status..." />;
  }

  const isVerified = current?.status === 'VERIFIED';

  return (
    <div className={styles.page}>
      <PageHeader
        title="KYC Verification"
        description="Verify your identity to enable claim filing"
      />

      {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
      {success && <Alert type="success" onDismiss={() => setSuccess('')}>{success}</Alert>}

      {current ? (
        <Card>
          <CardHeader>
            <CardTitle>Current Verification</CardTitle>
          </CardHeader>
          <CardContent>
            <div className={styles.statusRow}>
              <div>
                <p className={styles.docType}>{KYC_DOCUMENT_TYPE_LABELS[current.documentType]}</p>
                <p className={styles.docNumber}>{current.maskedDocumentNumber}</p>
                <p className={styles.docName}>{current.fullNameOnDocument}</p>
                <p className={styles.meta}>
                  Submitted: {formatDateTime(current.submittedAt)}
                </p>
                {current.verifiedAt && (
                  <p className={styles.meta}>
                    Verified: {formatDateTime(current.verifiedAt)}
                  </p>
                )}
              </div>
              <Badge
                variant={
                  isVerified
                    ? 'success'
                    : current.status === 'REJECTED'
                      ? 'danger'
                      : current.status === 'EXPIRED'
                        ? 'secondary'
                        : 'warning'
                }
              >
                {current.statusLabel}
              </Badge>
            </div>

            {!isVerified && current.resubmissionAllowed && (
              <div className={styles.resubmit}>
                <Button variant="secondary" onClick={() => setCurrent(null)}>
                  Submit Again
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Submit Verification</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit}>
              <div className={styles.formGrid}>
                <Select
                  label="Document Type"
                  options={Object.entries(KYC_DOCUMENT_TYPE_LABELS).map(([value, label]) => ({
                    value,
                    label,
                  }))}
                  value={documentType}
                  onChange={(e) => setDocumentType(e.target.value as KycDocumentType)}
                  required
                />
                <Input
                  label="Document Number"
                  value={documentNumber}
                  onChange={(e) => setDocumentNumber(e.target.value)}
                  placeholder="123456789V"
                  required
                  maxLength={40}
                />
                <Input
                  label="Full Name on Document"
                  value={fullNameOnDocument}
                  onChange={(e) => setFullNameOnDocument(e.target.value)}
                  placeholder="As printed on your ID"
                  required
                />
                <Input
                  label="Date of Birth"
                  type="date"
                  value={dateOfBirth}
                  max={new Date().toISOString().slice(0, 10)}
                  onChange={(e) => setDateOfBirth(e.target.value)}
                />
              </div>

              <div className={styles.uploads}>
                <label className={styles.uploadItem}>
                  <span className={styles.uploadTitle}>Front image *</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/heic"
                    onChange={(e) => setFrontImage(e.target.files?.[0] || null)}
                  />
                  <span className={styles.uploadHint}>
                    {frontImage ? frontImage.name : 'Choose image'}
                  </span>
                </label>
                <label className={styles.uploadItem}>
                  <span className={styles.uploadTitle}>Back image</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/heic"
                    onChange={(e) => setBackImage(e.target.files?.[0] || null)}
                  />
                  <span className={styles.uploadHint}>
                    {backImage ? backImage.name : 'Optional'}
                  </span>
                </label>
                <label className={styles.uploadItem}>
                  <span className={styles.uploadTitle}>Selfie</span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/heic"
                    onChange={(e) => setSelfie(e.target.files?.[0] || null)}
                  />
                  <span className={styles.uploadHint}>
                    {selfie ? selfie.name : 'Optional'}
                  </span>
                </label>
              </div>

              <div className={styles.submitRow}>
                <Button type="submit" loading={submitting} disabled={submitting}>
                  Submit Verification
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}