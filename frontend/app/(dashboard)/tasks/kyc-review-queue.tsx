'use client';

import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Dialog, PageLoader } from '@/lib/components/ui';
import { approveKyc, getAdminKycVerifications, getKycImageUrl, getKycReviewDocumentNumber, rejectKyc } from '@/lib/services/kyc';
import { formatDateTime } from '@/lib/types/claim';
import type { KycImageSide, KycVerificationResponse } from '@/lib/types/kyc';
import type { PageResponse } from '@/lib/types/page';
import type { QueueStatus } from './tasks-overview';
import styles from './tasks.module.css';

const PAGE_SIZE = 10;

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Unable to complete the request.';
}

export function KycReviewQueue({ onStatusChange }: { onStatusChange: (status: QueueStatus) => void }) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PageResponse<KycVerificationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [selected, setSelected] = useState<number | null>(null);
  const [reason, setReason] = useState('');
  const [resubmitAllowed, setResubmitAllowed] = useState(true);
  const [busy, setBusy] = useState(false);
  const [side, setSide] = useState<KycImageSide | null>(null);
  const [preview, setPreview] = useState<{
    id: number;
    side: KycImageSide;
    attempt: number;
    url?: string;
    contentType?: string;
    error?: string;
  } | null>(null);
  const [previewAttempt, setPreviewAttempt] = useState(0);
  const [documentNumber, setDocumentNumber] = useState<{ id: number; attempt: number; value?: string; error?: string } | null>(null);
  const [numberAttempt, setNumberAttempt] = useState(0);
  const [announcement, setAnnouncement] = useState('');

  const load = useCallback(async () => {
    onStatusChange('loading');
    setLoading(true);
    setError('');
    try {
      const data = await getAdminKycVerifications(page, PAGE_SIZE);
      if (page > 0 && data.content.length === 0) {
        setPage(page - 1);
      } else {
        setResult(data);
        onStatusChange(data.totalElements ? 'has-tasks' : 'empty');
      }
    } catch (err) {
      setError(errorMessage(err));
      onStatusChange('error');
    } finally {
      setLoading(false);
    }
  }, [page, onStatusChange]);

  useEffect(() => {
    let active = true;
    getAdminKycVerifications(page, PAGE_SIZE)
      .then((data) => {
        if (!active) return;
        if (page > 0 && data.content.length === 0) setPage(page - 1);
        else {
          setResult(data);
          onStatusChange(data.totalElements ? 'has-tasks' : 'empty');
        }
        setError('');
      })
      .catch((err: unknown) => {
        if (active) {
          setError(errorMessage(err));
          onStatusChange('error');
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, onStatusChange]);

  useEffect(() => {
    const refresh = () => {
      if (busy) return;
      setSelected(null);
      setSide(null);
      setDocumentNumber(null);
      if (page === 0) void load();
      else {
        setLoading(true);
        setPage(0);
      }
    };
    window.addEventListener('kyc-review-requested', refresh);
    return () => window.removeEventListener('kyc-review-requested', refresh);
  }, [busy, load, page]);

  useEffect(() => {
    if (selected === null || side === null) return;
    let active = true;
    let objectUrl: string | null = null;
    getKycImageUrl(selected, side)
      .then((image) => {
        objectUrl = image.url;
        if (active) setPreview({ ...image, side, id: selected, attempt: previewAttempt });
        else URL.revokeObjectURL(image.url);
      })
      .catch((err: unknown) => {
        if (active) setPreview({ id: selected, side, attempt: previewAttempt, error: errorMessage(err) });
      });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [selected, side, previewAttempt]);

  useEffect(() => {
    if (selected === null) return;
    let active = true;
    getKycReviewDocumentNumber(selected)
      .then((data) => {
        if (active) setDocumentNumber({ id: selected, attempt: numberAttempt, value: data.documentNumber });
      })
      .catch((err: unknown) => {
        if (active) setDocumentNumber({ id: selected, attempt: numberAttempt, error: errorMessage(err) });
      });
    return () => { active = false; };
  }, [selected, numberAttempt]);

  function selectTask(task: KycVerificationResponse) {
    setSelected(task.id);
    setDocumentNumber(null);
    setSide(task.hasFrontImage ? 'FRONT' : task.hasBackImage ? 'BACK' : task.hasSelfie ? 'SELFIE' : null);
    setReason('');
    setResubmitAllowed(true);
    setActionError('');
    setPreview(null);
  }

  function closeDialog() {
    if (busy) return;
    setSelected(null);
    setSide(null);
    setDocumentNumber(null);
  }

  function selectSide(nextSide: KycImageSide) {
    if (nextSide === side) return;
    setPreview(null);
    setSide(nextSide);
  }

  async function decide(id: number, approved: boolean) {
    if (!approved && !reason.trim()) {
      setActionError('Enter a reason for rejection.');
      return;
    }
    setBusy(true);
    setActionError('');
    try {
      if (approved) {
        await approveKyc(id);
      } else {
        await rejectKyc(id, { reason: reason.trim(), resubmitAllowed });
      }
      setSelected(null);
      setSide(null);
      setDocumentNumber(null);
      setAnnouncement(`KYC #${id} ${approved ? 'approved' : 'rejected'}.`);
      window.dispatchEvent(new Event('kyc-review-updated'));
      await load();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const openTask = result?.content.find((task) => task.id === selected);
  const currentPreview = preview?.id === selected && preview.side === side && preview.attempt === previewAttempt ? preview : null;
  const currentNumber = documentNumber?.id === selected && documentNumber.attempt === numberAttempt ? documentNumber : null;

  return (
    <section className={`${styles.page} ${styles.queue}`} id="kyc-review" aria-labelledby="kyc-review-heading">
      <div className={styles.queueHeader}>
        <div>
          <h2 id="kyc-review-heading" className={styles.srOnly}>KYC review queue</h2>
        </div>
        <Button type="button" variant="secondary" size="sm" loading={loading} disabled={busy} onClick={() => void load()}>Refresh</Button>
      </div>
      <p className={styles.srOnly} role="status">{announcement}</p>
      {error && <Alert type="error">Unable to load KYC tasks: {error}</Alert>}
      {loading && !result ? <PageLoader message="Loading KYC tasks..." /> : result && (
        <>
          <ul className={styles.list}>
            {result.content.map((task) => (
              <li className={styles.item} key={task.id}>
                <button
                  type="button"
                  className={styles.taskSummary}
                  aria-haspopup="dialog"
                  disabled={busy || loading}
                  onClick={() => selectTask(task)}
                >
                  <span>KYC review</span>
                  <strong>{task.customerName}</strong>
                </button>
              </li>
            ))}
          </ul>
          {result.totalPages > 1 && (
            <div className={styles.pagination}>
              <Button type="button" variant="secondary" size="sm" disabled={page === 0 || busy || loading} onClick={() => { setSelected(null); setSide(null); setDocumentNumber(null); setLoading(true); setPage(page - 1); }}>Previous</Button>
              <span>Page {page + 1} of {result.totalPages}</span>
              <Button type="button" variant="secondary" size="sm" disabled={result.last || busy || loading} onClick={() => { setSelected(null); setSide(null); setDocumentNumber(null); setLoading(true); setPage(page + 1); }}>Next</Button>
            </div>
          )}
        </>
      )}
      <Dialog
        isOpen={!!openTask}
        onClose={closeDialog}
        title="KYC review"
        size="lg"
        closeOnOverlay={!busy}
        showCloseButton={!busy}
        classNames={{ content: styles.dialogContent }}
        footer={openTask && (
          <>
            <Button type="button" disabled={busy} onClick={() => void decide(openTask.id, true)}>Approve</Button>
            <Button type="button" variant="danger" disabled={busy} onClick={() => void decide(openTask.id, false)}>Reject</Button>
            <Button type="button" variant="secondary" disabled={busy} onClick={closeDialog}>Cancel</Button>
          </>
        )}
      >
        {openTask && (
          <div className={styles.kycDetails}>
            <div>
              <div className={styles.claimHeading}>
                <strong>KYC #{openTask.id}</strong>
              </div>
              <dl className={styles.details}>
                <div><dt>Customer</dt><dd>{openTask.customerName}</dd></div>
                <div><dt>Name on document</dt><dd>{openTask.fullNameOnDocument}</dd></div>
                <div>
                  <dt>ID number</dt>
                  <dd>
                    {!currentNumber && <span role="status">Loading ID number...</span>}
                    {currentNumber?.value}
                    {currentNumber?.error && (
                      <span role="alert">
                        Unable to load ID number: {currentNumber.error}{' '}
                        <Button type="button" variant="secondary" size="sm" onClick={() => setNumberAttempt((attempt) => attempt + 1)}>Retry</Button>
                      </span>
                    )}
                  </dd>
                </div>
                <div><dt>Submitted</dt><dd>{formatDateTime(openTask.updatedAt)}</dd></div>
                {openTask.dateOfBirth && <div><dt>Date of birth</dt><dd>{openTask.dateOfBirth}</dd></div>}
                {openTask.failureReason && <div><dt>Screening flag</dt><dd>{openTask.failureReason}</dd></div>}
              </dl>
            </div>
            <div>
              <div className={styles.reviewForm}>
                <span className={styles.assignmentLabel}>Submitted documents</span>
                <div className={styles.reviewActions}>
                  {openTask.hasFrontImage && <Button type="button" variant={side === 'FRONT' ? 'primary' : 'secondary'} size="sm" disabled={busy} aria-pressed={side === 'FRONT'} onClick={() => selectSide('FRONT')}>Front</Button>}
                  {openTask.hasBackImage && <Button type="button" variant={side === 'BACK' ? 'primary' : 'secondary'} size="sm" disabled={busy} aria-pressed={side === 'BACK'} onClick={() => selectSide('BACK')}>Back</Button>}
                  {openTask.hasSelfie && <Button type="button" variant={side === 'SELFIE' ? 'primary' : 'secondary'} size="sm" disabled={busy} aria-pressed={side === 'SELFIE'} onClick={() => selectSide('SELFIE')}>Selfie</Button>}
                </div>
                {!side && <p>No documents are available for this submission.</p>}
                {side && !currentPreview && <p role="status">Loading {side.toLowerCase()} document...</p>}
                {side && currentPreview?.error && (
                  <div role="alert">
                    <p>Unable to display the {side.toLowerCase()} document: {currentPreview.error}</p>
                    <Button type="button" variant="secondary" size="sm" onClick={() => setPreviewAttempt((attempt) => attempt + 1)}>Retry</Button>
                  </div>
                )}
                {currentPreview?.url && (
                  <>
                    {currentPreview.contentType === 'application/pdf' ? (
                      <iframe className={styles.preview} src={currentPreview.url} title={`${currentPreview.side.toLowerCase()} document for KYC #${openTask.id}`} sandbox="" />
                    ) : (
                      // Uploaded images are authenticated blob URLs, not public Next.js image sources.
                      // eslint-disable-next-line @next/next/no-img-element
                      <img className={styles.previewImage} src={currentPreview.url} alt={`${currentPreview.side.toLowerCase()} document for KYC #${openTask.id}`} onError={() => setPreview((current) => current && current.id === currentPreview.id && current.side === currentPreview.side && current.attempt === currentPreview.attempt ? { ...current, error: 'Your browser could not preview this file. Open it in a new tab instead.' } : current)} />
                    )}
                    <a className={styles.claimLink} href={currentPreview.url} target="_blank" rel="noopener noreferrer">Open document in a new tab</a>
                  </>
                )}
                <label htmlFor={`reason-${openTask.id}`}>Rejection reason</label>
                <textarea id={`reason-${openTask.id}`} value={reason} onChange={(event) => setReason(event.target.value)} disabled={busy} />
                <label><input type="checkbox" checked={resubmitAllowed} onChange={(event) => setResubmitAllowed(event.target.checked)} disabled={busy} /> Allow resubmission</label>
                {actionError && <p className={styles.actionError} role="alert">{actionError}</p>}
              </div>
            </div>
          </div>
        )}
      </Dialog>
    </section>
  );
}
