'use client';

import { FormEvent, KeyboardEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import {
  Alert,
  Badge,
  Button,
  Card,
  Dialog,
  EmptyState,
  Input,
  PageHeader,
  PageLoader,
} from '@/lib/components/ui';
import { deleteUser, getUser, setUserEnabled, updateUser } from '@/lib/services/admin';
import { getClaims } from '@/lib/services/claims';
import { getCustomerKycVerifications } from '@/lib/services/kyc';
import { getAdminPolicies } from '@/lib/services/policies';
import type { ClaimSummaryResponse, KycVerificationResponse, PolicyAdminResponse, UserDetail } from '@/lib/types';
import { formatCurrency, formatDateTime } from '@/lib/types/claim';
import { KYC_STATUS_LABELS } from '@/lib/types/kyc';
import styles from './customer-detail.module.css';

type CustomerTab = 'overview' | 'policies' | 'claims' | 'kyc';

interface EditFormState {
  fullName: string;
  phone: string;
  nic: string;
}

const TABS: { id: CustomerTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'policies', label: 'Policies' },
  { id: 'claims', label: 'Claims' },
  { id: 'kyc', label: 'KYC' },
];

function formatDate(value: string | null): string {
  return value ? formatDateTime(value) : 'Never';
}

function errorMessage(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Unable to load this customer data.';
}

export default function CustomerDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const customerId = Number(params.id);

  const [customer, setCustomer] = useState<UserDetail | null>(null);
  const [policies, setPolicies] = useState<PolicyAdminResponse[]>([]);
  const [claims, setClaims] = useState<ClaimSummaryResponse[]>([]);
  const [verifications, setVerifications] = useState<KycVerificationResponse[]>([]);
  const [tabErrors, setTabErrors] = useState<Partial<Record<CustomerTab, string>>>({});
  const [tab, setTab] = useState<CustomerTab>('overview');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [saving, setSaving] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState<EditFormState>({ fullName: '', phone: '', nic: '' });
  const [editError, setEditError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function loadCustomer() {
      setCustomer(null);
      setPolicies([]);
      setClaims([]);
      setVerifications([]);
      setTabErrors({});
      setError('');
      if (!Number.isSafeInteger(customerId) || customerId < 1) {
        setError('Invalid customer ID.');
        setLoading(false);
        return;
      }

      setLoading(true);
      try {
        const profile = await getUser(customerId);
        if (cancelled) return;
        setCustomer(profile);

        const [policyResult, claimResult, kycResult] = await Promise.allSettled([
          getAdminPolicies(0, 100, { customerId }),
          getClaims(0, 20, { customerId }),
          getCustomerKycVerifications(customerId),
        ]);
        if (cancelled) return;

        if (policyResult.status === 'fulfilled') {
          setPolicies(policyResult.value.content);
        } else {
          setTabErrors((previous) => ({ ...previous, policies: errorMessage(policyResult.reason) }));
        }
        if (claimResult.status === 'fulfilled') {
          setClaims(claimResult.value.content);
        } else {
          setTabErrors((previous) => ({ ...previous, claims: errorMessage(claimResult.reason) }));
        }
        if (kycResult.status === 'fulfilled') {
          setVerifications(kycResult.value);
        } else {
          setTabErrors((previous) => ({ ...previous, kyc: errorMessage(kycResult.reason) }));
        }
      } catch (err) {
        if (!cancelled) setError(errorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadCustomer();
    return () => {
      cancelled = true;
    };
  }, [customerId]);

  function openEdit() {
    if (!customer) return;
    setEditError('');
    setEditForm({
      fullName: customer.fullName,
      phone: customer.phone ?? '',
      nic: customer.nic ?? '',
    });
    setEditOpen(true);
  }

  async function handleEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!customer) return;
    if (!editForm.fullName.trim()) {
      setEditError('Full name is required.');
      return;
    }
    setSaving(true);
    setEditError('');
    setActionError('');
    try {
      const updated = await updateUser(customer.id, {
        fullName: editForm.fullName.trim(),
        phone: editForm.phone.trim() || null,
        nic: editForm.nic.trim() || null,
      });
      setCustomer(updated);
      setEditOpen(false);
    } catch (err) {
      setEditError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleEnabled() {
    if (!customer) return;
    setSaving(true);
    setActionError('');
    try {
      await setUserEnabled(customer.id, !customer.enabled);
      setCustomer((previous) => previous ? { ...previous, enabled: !previous.enabled } : previous);
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!customer || !window.confirm(`Delete ${customer.fullName}'s account? This cannot be undone.`)) return;
    setSaving(true);
    setActionError('');
    try {
      await deleteUser(customer.id);
      router.push('/customers');
    } catch (err) {
      setActionError(errorMessage(err));
      setSaving(false);
    }
  }

  function handleTabKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
    event.preventDefault();
    const currentIndex = TABS.findIndex((entry) => entry.id === tab);
    const direction = event.key === 'ArrowRight' ? 1 : -1;
    const nextTab = TABS[(currentIndex + direction + TABS.length) % TABS.length];
    setTab(nextTab.id);
    document.getElementById(`customer-tab-${nextTab.id}`)?.focus();
  }

  if (loading) return <PageLoader message="Loading customer..." />;
  if (!customer) {
    return (
      <div className={styles.page}>
        <Alert type="error">{error || 'Customer not found.'}</Alert>
        <Link className={styles.backLink} href="/customers">Back to customers</Link>
      </div>
    );
  }

  return (
    <section className={styles.page}>
      <Link className={styles.backLink} href="/customers">Back to customers</Link>
      {error && <Alert type="error">{error}</Alert>}
      {actionError && <Alert type="error" onDismiss={() => setActionError('')}>{actionError}</Alert>}
      <PageHeader
        title={customer.fullName}
        description={`${customer.email} · Customer #${customer.id}`}
        actions={
          <div className={styles.headerActions}>
            <Button type="button" variant="secondary" onClick={openEdit} disabled={saving}>Edit</Button>
            <Button type="button" variant="secondary" onClick={() => void handleToggleEnabled()} loading={saving}>
              {customer.enabled ? 'Disable account' : 'Enable account'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => void handleDelete()} loading={saving}>Delete</Button>
          </div>
        }
      />

      <div className={styles.tabList} role="tablist" aria-label="Customer information">
        {TABS.map((entry) => (
          <button
            key={entry.id}
            id={`customer-tab-${entry.id}`}
            className={styles.tab}
            type="button"
            role="tab"
            aria-selected={tab === entry.id}
            aria-controls="customer-panel"
            tabIndex={tab === entry.id ? 0 : -1}
            onClick={() => setTab(entry.id)}
            onKeyDown={handleTabKeyDown}
          >
            {entry.label}
          </button>
        ))}
      </div>

      <div
        id="customer-panel"
        className={styles.panel}
        role="tabpanel"
        aria-labelledby={`customer-tab-${tab}`}
        tabIndex={0}
      >
        {tab === 'overview' && (
          <Card>
            <div className={styles.infoGrid}>
              <InfoItem label="Full name" value={customer.fullName} />
              <InfoItem label="Email" value={customer.email} />
              <InfoItem label="Phone" value={customer.phone || 'Not provided'} />
              <InfoItem label="National ID" value={customer.nic || 'Not provided'} />
              <InfoItem label="Account role" value={customer.roleName || customer.role} />
              <InfoItem label="Account status">
                <Badge variant={customer.enabled ? 'success' : 'danger'}>
                  {customer.enabled ? 'Active' : 'Disabled'}
                </Badge>
              </InfoItem>
              <InfoItem label="Last login" value={formatDate(customer.lastLoginAt)} />
              <InfoItem label="Policies" value={String(policies.length)} />
              <InfoItem label="Claims loaded" value={String(claims.length)} />
              <InfoItem label="KYC records" value={String(verifications.length)} />
            </div>
          </Card>
        )}

        {tab === 'policies' && (
          tabErrors.policies ? <Alert type="error">{tabErrors.policies}</Alert> : policies.length === 0 ? (
            <EmptyState title="No policies" description="This customer has no policy records." />
          ) : (
            <Card padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Customer policies</caption>
                  <thead><tr><th>Policy</th><th>Product</th><th>Vehicle</th><th>Status</th><th>Coverage dates</th><th>Premium</th></tr></thead>
                  <tbody>
                    {policies.map((policy) => (
                      <tr key={policy.id}>
                        <td className={styles.primaryCell}>{policy.policyNumber}</td>
                        <td>{policy.productCode}</td>
                        <td>{policy.vehicle.registrationNumber}</td>
                        <td><Badge variant={policy.isCurrentlyValid ? 'success' : 'warning'}>{policy.status}</Badge></td>
                        <td>{formatDateTime(policy.startDate)} to {formatDateTime(policy.endDate)}</td>
                        <td>{formatCurrency(policy.premiumAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )
        )}

        {tab === 'claims' && (
          tabErrors.claims ? <Alert type="error">{tabErrors.claims}</Alert> : claims.length === 0 ? (
            <EmptyState title="No claims" description="This customer has no claim records." />
          ) : (
            <Card padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Customer claims</caption>
                  <thead><tr><th>Claim</th><th>Status</th><th>Incident</th><th>Vehicle</th><th>Policy</th><th>Submitted</th><th>Estimate</th></tr></thead>
                  <tbody>
                    {claims.map((claim) => (
                      <tr key={claim.id}>
                        <td className={styles.primaryCell}><Link href={`/claims/${claim.id}`}>{claim.claimNumber}</Link></td>
                        <td><Badge variant="info">{claim.statusLabel}</Badge></td>
                        <td>{claim.incidentType}</td>
                        <td>{claim.vehicleRegistration}</td>
                        <td>{claim.policyNumber}</td>
                        <td>{formatDateTime(claim.submittedAt)}</td>
                        <td>{claim.estimatedAmount === null ? '-' : formatCurrency(claim.estimatedAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )
        )}

        {tab === 'kyc' && (
          tabErrors.kyc ? <Alert type="error">{tabErrors.kyc}</Alert> : verifications.length === 0 ? (
            <EmptyState title="No KYC records" description="This customer has not submitted identity documents." />
          ) : (
            <Card padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Customer identity verification records</caption>
                  <thead><tr><th>Document</th><th>Document number</th><th>Status</th><th>Submitted</th><th>Verified</th><th>Validity</th></tr></thead>
                  <tbody>
                    {verifications.map((verification) => (
                      <tr key={verification.id}>
                        <td className={styles.primaryCell}>{verification.documentLabel}</td>
                        <td>{verification.maskedDocumentNumber}</td>
                        <td><Badge variant={verification.status === 'VERIFIED' ? 'success' : verification.status === 'REJECTED' ? 'danger' : 'warning'}>{KYC_STATUS_LABELS[verification.status]}</Badge></td>
                        <td>{formatDateTime(verification.submittedAt)}</td>
                        <td>{formatDate(verification.verifiedAt)}</td>
                        <td>{verification.stillValid ? 'Valid' : 'Expired'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )
        )}
      </div>

      <Dialog
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        title={`Edit ${customer.fullName}`}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setEditOpen(false)}>Cancel</Button>
            <Button type="submit" form="edit-customer-form" loading={saving}>Save changes</Button>
          </>
        }
      >
        <form id="edit-customer-form" className={styles.dialogForm} onSubmit={handleEdit}>
          {editError && <Alert type="error">{editError}</Alert>}
          <Input label="Full name" value={editForm.fullName} onChange={(event) => setEditForm((previous) => ({ ...previous, fullName: event.target.value }))} maxLength={160} required />
          <Input label="Phone" value={editForm.phone} onChange={(event) => setEditForm((previous) => ({ ...previous, phone: event.target.value }))} />
          <Input label="National ID" value={editForm.nic} onChange={(event) => setEditForm((previous) => ({ ...previous, nic: event.target.value }))} maxLength={32} />
        </form>
      </Dialog>
    </section>
  );
}

function InfoItem({ label, value, children }: { label: string; value?: string; children?: React.ReactNode }) {
  return (
    <div className={styles.infoItem}>
      <span className={styles.infoLabel}>{label}</span>
      <div className={styles.infoValue}>{children ?? value}</div>
    </div>
  );
}