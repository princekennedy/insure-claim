'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardFooter,
  Button,
  Input,
  Select,
  Textarea,
  Alert,
  PageLoader,
  EmptyState,
} from '@/lib/components/ui';
import { getVehicles } from '@/lib/services/vehicles';
import { getPolicies } from '@/lib/services/policies';
import { fileClaim } from '@/lib/services/claims';
import { INCIDENT_TYPE_LABELS } from '@/lib/types/claim';
import type { VehicleResponse, PolicyResponse, IncidentType } from '@/lib/types';
import styles from '../claims.module.css';

export default function NewClaimPage() {
  const router = useRouter();
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]);
  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState({
    vehicleId: '',
    incidentType: '',
    incidentDate: new Date().toISOString().slice(0, 10),
    incidentLocation: '',
    description: '',
    estimatedAmount: '',
    reportedByPolice: false,
    thirdPartyInvolved: false,
    requiresKyc: true,
  });

  useEffect(() => {
    async function setup() {
      try {
        const [vehicleList, policyList] = await Promise.all([
          getVehicles(),
          getPolicies(),
        ]);
        setVehicles(vehicleList);
        setPolicies(policyList.filter((p) => p.isCurrentlyValid));
      } catch (err) {
        console.error('Failed to load claim setup:', err);
        setError('Failed to load your vehicles and policies');
      } finally {
        setLoading(false);
      }
    }
    setup();
  }, []);

  const policyForVehicle = (vehicleId: string): PolicyResponse | undefined =>
    policies.find((p) => p.vehicle.id === Number(vehicleId));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.vehicleId || !form.incidentType) {
      setError('Please select a vehicle and incident type');
      return;
    }

    setSubmitting(true);
    try {
      const result = await fileClaim({
        vehicleId: Number(form.vehicleId),
        incidentType: form.incidentType as IncidentType,
        incidentDate: form.incidentDate,
        incidentLocation: form.incidentLocation || undefined,
        description: form.description,
        estimatedAmount: form.estimatedAmount ? Number(form.estimatedAmount) : undefined,
        reportedByPolice: form.reportedByPolice,
        thirdPartyInvolved: form.thirdPartyInvolved,
        requiresKyc: form.requiresKyc,
      });
      router.push(`/claims/${result.summary.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to file claim');
      setSubmitting(false);
    }
  };

  if (loading) {
    return <PageLoader message="Preparing claim form..." />;
  }

  if (vehicles.length === 0) {
    return (
      <div className={styles.page}>
        <PageHeader
          title="File a Claim"
          description="You need a registered vehicle to file a claim"
        />
        <EmptyState
          title="No vehicles registered"
          description="Add a vehicle to your profile before filing a claim."
          action={{
            label: 'Add Vehicle',
            onClick: () => router.push('/vehicles'),
          }}
        />
      </div>
    );
  }

  const validVehicles = vehicles.filter((v) => policyForVehicle(String(v.id)));
  const ownedVehicles = validVehicles.length > 0 ? validVehicles : vehicles;

  return (
    <div className={styles.page}>
      <PageHeader
        title="File a Claim"
        description="Provide details about the incident to start your claim"
        actions={
          <Link href="/claims" className={styles.newButton}>
            <Button variant="secondary">Back to Claims</Button>
          </Link>
        }
      />

      {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}

      <form onSubmit={handleSubmit}>
        <Card>
          <CardHeader>
            <CardTitle>Incident Details</CardTitle>
          </CardHeader>
          <CardContent>
            <div className={styles.formGrid}>
              <Select
                label="Vehicle"
                options={ownedVehicles.map((v) => ({
                  value: String(v.id),
                  label: `${v.registrationNumber} - ${v.make} ${v.model} (${v.year})`,
                }))}
                value={form.vehicleId}
                onChange={(e) => setForm({ ...form, vehicleId: e.target.value })}
                placeholder="Select vehicle"
                required
              />

              {form.vehicleId && policyForVehicle(form.vehicleId) && (
                <Input
                  label="Policy"
                  value={policyForVehicle(form.vehicleId)?.policyNumber || ''}
                  readOnly
                  hint={`Cover: ${policyForVehicle(form.vehicleId)?.productCode}`}
                />
              )}

              <Select
                label="Incident Type"
                options={Object.entries(INCIDENT_TYPE_LABELS).map(([value, label]) => ({
                  value,
                  label,
                }))}
                value={form.incidentType}
                onChange={(e) => setForm({ ...form, incidentType: e.target.value })}
                placeholder="Select incident type"
                required
              />

              <Input
                label="Incident Date"
                type="date"
                value={form.incidentDate}
                max={new Date().toISOString().slice(0, 10)}
                onChange={(e) => setForm({ ...form, incidentDate: e.target.value })}
                required
              />

              <Input
                label="Incident Location"
                type="text"
                value={form.incidentLocation}
                onChange={(e) => setForm({ ...form, incidentLocation: e.target.value })}
                placeholder="Road, town or landmark"
              />

              <Input
                label="Estimated Damage (LKR)"
                type="number"
                min="0"
                step="0.01"
                value={form.estimatedAmount}
                onChange={(e) => setForm({ ...form, estimatedAmount: e.target.value })}
                placeholder="0.00"
              />
            </div>

            <Textarea
              label="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Describe what happened (min 10 characters)"
              rows={5}
              minLength={10}
              maxLength={4000}
              required
            />

            <div className={styles.checkRow}>
              <label className={styles.checkLabel}>
                <input
                  type="checkbox"
                  checked={form.reportedByPolice}
                  onChange={(e) => setForm({ ...form, reportedByPolice: e.target.checked })}
                />
                Incident was reported to police
              </label>

              <label className={styles.checkLabel}>
                <input
                  type="checkbox"
                  checked={form.thirdPartyInvolved}
                  onChange={(e) => setForm({ ...form, thirdPartyInvolved: e.target.checked })}
                />
                Third party involved
              </label>

              <label className={styles.checkLabel}>
                <input
                  type="checkbox"
                  checked={form.requiresKyc}
                  onChange={(e) => setForm({ ...form, requiresKyc: e.target.checked })}
                />
                KYC verification required
              </label>
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit" loading={submitting} disabled={submitting}>
              Submit Claim
            </Button>
          </CardFooter>
        </Card>
      </form>
    </div>
  );
}