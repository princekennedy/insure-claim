'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  ProgressBar,
  Button,
  PageLoader,
  EmptyState,
  Select,
  Input,
  Alert,
} from '@/lib/components/ui';
import {
  getClaim,
  updateClaimStatus,
  uploadDocument,
  deleteDocument,
} from '@/lib/services/claims';
import {
  formatCurrency,
  formatDateTime,
  CLAIM_DOCUMENT_TYPE_LABELS,
  INCIDENT_TYPE_LABELS,
} from '@/lib/types/claim';
import type {
  ClaimDetailResponse,
  ClaimStatus,
  ClaimDocumentType,
  UpdateClaimStatusRequest,
} from '@/lib/types/claim';
import styles from './claim-detail.module.css';

const FINAL_STATES: ClaimStatus[] = ['REJECTED', 'CLOSED', 'WITHDRAWN'];

function statusVariant(status: ClaimStatus): 'success' | 'danger' | 'info' {
  if (status === 'SETTLED' || status === 'CLOSED') return 'success';
  if (status === 'REJECTED' || status === 'WITHDRAWN') return 'danger';
  return 'info';
}

export default function ClaimDetailPage() {
  const params = useParams();
  const claimId = Number(params.id);

  const [claim, setClaim] = useState<ClaimDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [statusLoading, setStatusLoading] = useState(false);
  const [documentLoading, setDocumentLoading] = useState(false);
  const [documentType, setDocumentType] = useState<ClaimDocumentType>('DAMAGE_PHOTO');
  const [targetStatus, setTargetStatus] = useState('');
  const [statusNote, setStatusNote] = useState('');
  const [approvedAmount, setApprovedAmount] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    async function fetchClaim() {
      try {
        const data = await getClaim(claimId);
        setClaim(data);
      } catch (err) {
        console.error('Failed to load claim:', err);
        setError('Failed to load claim details');
      } finally {
        setLoading(false);
      }
    }
    fetchClaim();
  }, [claimId]);

  const handleStatusUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatusLoading(true);
    setError('');
    setSuccess('');

    try {
      const updateData: UpdateClaimStatusRequest = {
        status: targetStatus as ClaimStatus,
      };
      if (statusNote) updateData.note = statusNote;
      if (approvedAmount) updateData.approvedAmount = Number(approvedAmount);

      const updated = await updateClaimStatus(claimId, updateData);
      setClaim(updated);
      setSuccess('Status updated successfully');
      setTargetStatus('');
      setStatusNote('');
      setApprovedAmount('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update status');
    } finally {
      setStatusLoading(false);
    }
  };

  const handleDocumentUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setDocumentLoading(true);
    try {
      const result = await uploadDocument(claimId, file, documentType);
      setClaim((prev) =>
        prev ? { ...prev, documents: [...prev.documents, result] } : prev,
      );
      setSuccess('Document uploaded successfully');
      e.target.value = '';
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload document');
    } finally {
      setDocumentLoading(false);
    }
  };

  const handleDeleteDocument = async (documentId: number) => {
    try {
      await deleteDocument(claimId, documentId);
      setClaim((prev) =>
        prev
          ? { ...prev, documents: prev.documents.filter((doc) => doc.id !== documentId) }
          : prev,
      );
      setSuccess('Document deleted');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete document');
    }
  };

  if (loading) {
    return <PageLoader message="Loading claim..." />;
  }

  if (!claim) {
    return (
      <EmptyState
        title="Claim not found"
        description="The claim you're looking for doesn't exist or you don't have access to it."
      />
    );
  }

  const { summary, timeline, documents, nextStages, repair, fraudAlerts, canSubmitFeedback } = claim;

  return (
    <div className={styles.page}>
      {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
      {success && <Alert type="success" onDismiss={() => setSuccess('')}>{success}</Alert>}

      <PageHeader
        title={summary.claimNumber}
        description={`${INCIDENT_TYPE_LABELS[summary.incidentType] || summary.incidentType} · ${formatDateTime(summary.submittedAt)}`}
      />

      <div className={styles.grid}>
        <div className={styles.mainColumn}>
          <Card>
            <CardHeader>
              <CardTitle>Claim Details</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={styles.detailGrid}>
                <DetailRow label="Status">
                  <Badge variant={statusVariant(summary.status)}>
                    {summary.statusLabel}
                  </Badge>
                </DetailRow>
                <DetailRow label="Incident Type">
                  {INCIDENT_TYPE_LABELS[summary.incidentType] || summary.incidentType}
                </DetailRow>
                <DetailRow label="Incident Date">
                  {claim.incidentLocalDate}
                </DetailRow>
                {claim.incidentLocation && (
                  <DetailRow label="Location">{claim.incidentLocation}</DetailRow>
                )}
                {claim.description && (
                  <DetailRow label="Description">{claim.description}</DetailRow>
                )}
                <DetailRow label="Policy Number">{summary.policyNumber}</DetailRow>
                <DetailRow label="Vehicle">{summary.vehicleLabel}</DetailRow>
                <DetailRow label="Reported by Police">
                  {claim.reportedByPolice ? 'Yes' : 'No'}
                </DetailRow>
                <DetailRow label="Third Party Involved">
                  {claim.thirdPartyInvolved ? 'Yes' : 'No'}
                </DetailRow>
                <DetailRow label="Fraud Flagged">
                  {summary.isFraudFlagged ? (
                    <Badge variant="danger">Yes</Badge>
                  ) : (
                    'No'
                  )}
                </DetailRow>
                <DetailRow label="KYC Required">
                  {summary.requiresKyc ? (
                    <Badge variant="warning">Yes</Badge>
                  ) : (
                    'No'
                  )}
                </DetailRow>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Financial Summary</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={styles.financialGrid}>
                <FinancialItem label="Estimated Amount" value={summary.estimatedAmount} />
                <FinancialItem label="Approved Amount" value={summary.approvedAmount} />
                <FinancialItem label="Excess" value={summary.excessAmount} />
                <FinancialItem label="Net Settlement" value={summary.netSettlement} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Documents</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={styles.documentsList}>
                {documents.length > 0 ? (
                  documents.map((doc) => (
                    <div key={doc.id} className={styles.documentItem}>
                      <div className={styles.documentInfo}>
                        <span className={styles.documentType}>
                          {CLAIM_DOCUMENT_TYPE_LABELS[doc.documentType] || doc.documentType}
                        </span>
                        <span className={styles.documentName}>{doc.fileName}</span>
                        <span className={styles.documentSize}>{doc.sizeLabel}</span>
                        <span className={styles.documentDate}>
                          {formatDateTime(doc.uploadedAt)}
                        </span>
                      </div>
                      <div className={styles.documentActions}>
                        {doc.downloadUrl && (
                          <a
                            href={doc.downloadUrl}
                            target="_blank"
                            rel="noreferrer"
                            className={styles.downloadButton}
                          >
                            Download
                          </a>
                        )}
                        <button
                          onClick={() => handleDeleteDocument(doc.id)}
                          className={styles.deleteButton}
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className={styles.noDocuments}>No documents uploaded yet</p>
                )}
              </div>

              <div className={styles.uploadSection}>
                <Select
                  label="Document Type"
                  options={Object.entries(CLAIM_DOCUMENT_TYPE_LABELS).map(([value, label]) => ({
                    value,
                    label,
                  }))}
                  value={documentType}
                  onChange={(e) => setDocumentType(e.target.value as ClaimDocumentType)}
                />
                <div className={styles.fileUpload}>
                  <input
                    type="file"
                    id="documentUpload"
                    accept="image/jpeg,image/png,image/webp,image/heic,application/pdf"
                    onChange={handleDocumentUpload}
                    disabled={documentLoading}
                    className={styles.fileInput}
                  />
                  <label htmlFor="documentUpload" className={styles.fileLabel}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-5 h-5">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <polyline points="17 8 12 3 7 8" />
                      <line x1="12" y1="3" x2="12" y2="15" />
                    </svg>
                    {documentLoading ? 'Uploading...' : 'Upload Document'}
                  </label>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className={styles.sideColumn}>
          <Card>
            <CardHeader>
              <CardTitle>Claim Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={styles.progressCard}>
                <ProgressBar
                  value={summary.progressPercent}
                  size="lg"
                  showPercentage
                  label="Overall Progress"
                />
              </div>
            </CardContent>
          </Card>

          {nextStages.length > 0 && !FINAL_STATES.includes(summary.status) && (
            <Card>
              <CardHeader>
                <CardTitle>Update Status</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleStatusUpdate} className={styles.statusForm}>
                  <Select
                    label="Next Stage"
                    options={nextStages.map((stage) => ({
                      value: stage.status,
                      label: stage.label,
                    }))}
                    value={targetStatus}
                    onChange={(e) => setTargetStatus(e.target.value)}
                    placeholder="Select next stage"
                  />
                  <Input
                    label="Approved Amount (optional)"
                    type="number"
                    min="0"
                    step="0.01"
                    value={approvedAmount}
                    onChange={(e) => setApprovedAmount(e.target.value)}
                    placeholder="0.00"
                  />
                  <Input
                    label="Note (optional)"
                    type="text"
                    value={statusNote}
                    onChange={(e) => setStatusNote(e.target.value)}
                    placeholder="Add a note"
                  />
                  <Button type="submit" fullWidth loading={statusLoading} disabled={!targetStatus}>
                    Update Status
                  </Button>
                </form>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={styles.timeline}>
                {timeline.length > 0 ? (
                  timeline.map((entry, index) => (
                    <div
                      key={index}
                      className={`${styles.timelineItem} ${entry.isCurrent ? styles.current : ''}`}
                    >
                      <div className={styles.timelineDot} />
                      <div className={styles.timelineContent}>
                        <span className={styles.timelineLabel}>{entry.label}</span>
                        {entry.note && (
                          <span className={styles.timelineNote}>{entry.note}</span>
                        )}
                        <span className={styles.timelineDate}>
                          {formatDateTime(entry.occurredAt)}
                        </span>
                        <span className={styles.timelineActor}>
                          by {entry.actorLabel}
                        </span>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className={styles.noTimeline}>No timeline entries yet</p>
                )}
              </div>
            </CardContent>
          </Card>

          {repair && (
            <Card>
              <CardHeader>
                <CardTitle>Repair Job</CardTitle>
              </CardHeader>
              <CardContent>
                <div className={styles.repairInfo}>
                  <div className={styles.repairHeader}>
                    <span className={styles.repairCode}>{repair.referenceCode}</span>
                    <Badge variant="info">{repair.statusLabel}</Badge>
                  </div>
                  <ProgressBar value={repair.progressPercent} size="sm" showPercentage />
                  <div className={styles.repairDetails}>
                    <p><strong>Garage:</strong> {repair.garageName}</p>
                    <p><strong>Phone:</strong> {repair.garagePhone}</p>
                    <p><strong>Address:</strong> {repair.garageAddress}</p>
                    {repair.quotedAmount !== null && repair.quotedAmount !== undefined && (
                      <p><strong>Quoted:</strong> {formatCurrency(repair.quotedAmount)}</p>
                    )}
                    {repair.finalAmount !== null && repair.finalAmount !== undefined && (
                      <p><strong>Final:</strong> {formatCurrency(repair.finalAmount)}</p>
                    )}
                    {repair.notes && <p><strong>Notes:</strong> {repair.notes}</p>}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {canSubmitFeedback && (
            <Card>
              <CardHeader>
                <CardTitle>Submit Feedback</CardTitle>
              </CardHeader>
              <CardContent>
                <p className={styles.feedbackNote}>
                  {claim.repair?.garageName
                    ? `Rate your experience with ${claim.repair.garageName}`
                    : 'Rate your repair experience'}
                </p>
                <Button fullWidth>Rate This Garage</Button>
              </CardContent>
            </Card>
          )}

          {fraudAlerts.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Fraud Alerts</CardTitle>
              </CardHeader>
              <CardContent>
                <div className={styles.fraudAlerts}>
                  {fraudAlerts.map((alert) => (
                    <div key={alert.id} className={styles.fraudAlert}>
                      <Badge
                        variant={
                          alert.severity === 'CRITICAL' || alert.severity === 'HIGH'
                            ? 'danger'
                            : alert.severity === 'MEDIUM'
                            ? 'warning'
                            : 'info'
                        }
                      >
                        {alert.severity}
                      </Badge>
                      <div className={styles.fraudAlertContent}>
                        <span className={styles.fraudRule}>{alert.ruleCode}</span>
                        <span className={styles.fraudDesc}>{alert.description}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className={styles.detailRow}>
      <span className={styles.detailLabel}>{label}</span>
      <span className={styles.detailValue}>{children}</span>
    </div>
  );
}

function FinancialItem({
  label,
  value,
}: {
  label: string;
  value: number | null | undefined;
}) {
  return (
    <div className={styles.financialItem}>
      <span className={styles.financialLabel}>{label}</span>
      <span className={styles.financialValue}>
        {value !== null && value !== undefined ? formatCurrency(value) : '-'}
      </span>
    </div>
  );
}