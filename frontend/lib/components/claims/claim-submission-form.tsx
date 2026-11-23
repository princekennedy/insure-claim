'use client';

import { useEffect, useState } from 'react';
import {
  Button,
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
  EmptyState,
  Input,
  PageLoader,
  Select,
  Textarea,
  Alert,
} from '@/lib/components/ui';
import { getVehicles } from '@/lib/services/vehicles';
import { getPolicies } from '@/lib/services/policies';
import { fileClaim } from '@/lib/services/claims';
import { INCIDENT_TYPE_LABELS } from '@/lib/types/claim';
import type { VehicleResponse, PolicyResponse, IncidentType } from '@/lib/types';
import styles from './claim-submission-form.module.css';

interface ClaimSubmissionFormProps {
  onSuccess: (claimId: number) => void;
  onAddVehicle: () => void;
}

export function ClaimSubmissionForm({
  onSuccess,
  onAddVehicle,
}: ClaimSubmissionFormProps) {
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
        setPolicies(policyList.filter((policy) => policy.isCurrentlyValid));
      } catch (err) {
        console.error('Failed to load claim setup:', err);
        setError('Failed to load your vehicles and policies');
      } finally {
        setLoading(false);
      }
    }
    void setup();
  }, []);

  const policyForVehicle = (vehicleId: string): PolicyResponse | undefined =>
    policies.find((policy) => policy.vehicle.id === Number(vehicleId));

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
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
      onSuccess(result.summary.id);
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
      <>
        {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
        <EmptyState
          title="No vehicles registered"
          description="Add a vehicle to your profile before filing a claim."
          action={{ label: 'Add Vehicle', onClick: onAddVehicle }}
        />
      </>
    );
  }

  const validVehicles = vehicles.filter((vehicle) => policyForVehicle(String(vehicle.id)));
  const ownedVehicles = validVehicles.length > 0 ? validVehicles : vehicles;

  return (
    <>
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
                options={ownedVehicles.map((vehicle) => ({
                  value: String(vehicle.id),
                  label: `${vehicle.registrationNumber} - ${vehicle.make} ${vehicle.model} (${vehicle.year})`,
                }))}
                value={form.vehicleId}
                onChange={(event) => setForm({ ...form, vehicleId: event.target.value })}
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
                onChange={(event) => setForm({ ...form, incidentType: event.target.value })}
                placeholder="Select incident type"
                required
              />

              <Input
                label="Incident Date"
                type="date"
                value={form.incidentDate}
                max={new Date().toISOString().slice(0, 10)}
                onChange={(event) => setForm({ ...form, incidentDate: event.target.value })}
                required
              />

              <Input
                label="Incident Location"
                type="text"
                value={form.incidentLocation}
                onChange={(event) => setForm({ ...form, incidentLocation: event.target.value })}
                placeholder="Road, town or landmark"
              />

              <Input
                label="Estimated Damage (LKR)"
                type="number"
                min="0"
                step="0.01"
                value={form.estimatedAmount}
                onChange={(event) => setForm({ ...form, estimatedAmount: event.target.value })}
                placeholder="0.00"
              />
            </div>

            <Textarea
              label="Description"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
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
                  onChange={(event) => setForm({ ...form, reportedByPolice: event.target.checked })}
                />
                Incident was reported to police
              </label>

              <label className={styles.checkLabel}>
                <input
                  type="checkbox"
                  checked={form.thirdPartyInvolved}
                  onChange={(event) => setForm({ ...form, thirdPartyInvolved: event.target.checked })}
                />
                Third party involved
              </label>

              <label className={styles.checkLabel}>
                <input
                  type="checkbox"
                  checked={form.requiresKyc}
                  onChange={(event) => setForm({ ...form, requiresKyc: event.target.checked })}
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
    </>
  );
}
