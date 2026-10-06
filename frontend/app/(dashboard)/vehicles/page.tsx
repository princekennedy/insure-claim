'use client';

import { useState, useEffect, useCallback } from 'react';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Dialog,
  Button,
  Input,
  Badge,
  EmptyState,
  PageLoader,
  Alert,
} from '@/lib/components/ui';
import { getVehicles, createVehicle, deleteVehicle } from '@/lib/services/vehicles';
import type { VehicleResponse, VehicleRequest } from '@/lib/types';
import styles from './vehicles.module.css';

const EMPTY_FORM: VehicleRequest = {
  registrationNumber: '',
  make: '',
  model: '',
  year: new Date().getFullYear(),
  color: '',
  chassisNumber: '',
  engineNumber: '',
};

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<VehicleRequest>(EMPTY_FORM);

  const loadVehicles = useCallback(async () => {
    try {
      const data = await getVehicles();
      setVehicles(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load vehicles');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getVehicles()
      .then((data) => setVehicles(data))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load vehicles'))
      .finally(() => setLoading(false));
  }, []);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await createVehicle(form);
      setSuccess('Vehicle added successfully');
      setIsAddOpen(false);
      setForm(EMPTY_FORM);
      await loadVehicles();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add vehicle');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (vehicleId: number) => {
    setError('');
    try {
      await deleteVehicle(vehicleId);
      setSuccess('Vehicle deleted');
      setVehicles((prev) => prev.filter((v) => v.id !== vehicleId));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete vehicle');
    }
  };

  if (loading) {
    return <PageLoader message="Loading vehicles..." />;
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Vehicles"
        description="Manage your registered vehicles"
        actions={<Button onClick={() => setIsAddOpen(true)}>Add Vehicle</Button>}
      />

      {error && <Alert type="error" onDismiss={() => setError('')}>{error}</Alert>}
      {success && <Alert type="success" onDismiss={() => setSuccess('')}>{success}</Alert>}

      {vehicles.length === 0 ? (
        <EmptyState
          title="No vehicles registered"
          description="Add your first vehicle to start filing claims."
          action={{ label: 'Add Vehicle', onClick: () => setIsAddOpen(true) }}
        />
      ) : (
        <div className={styles.grid}>
          {vehicles.map((vehicle) => (
            <Card key={vehicle.id} className={styles.vehicleCard}>
              <CardHeader>
                <CardTitle className={styles.reg}>{vehicle.registrationNumber}</CardTitle>
                <Badge variant="secondary">
                  {vehicle.year} · {vehicle.make}
                </Badge>
              </CardHeader>
              <CardContent>
                <p className={styles.model}>
                  {vehicle.model}
                </p>
                <p className={styles.detail}>
                  <strong>Color:</strong> {vehicle.color || '—'}
                </p>
                <p className={styles.detail}>
                  <strong>Chassis:</strong> {vehicle.chassisNumber || '—'}
                </p>
                <p className={styles.detail}>
                  <strong>Engine:</strong> {vehicle.engineNumber || '—'}
                </p>
              </CardContent>
              <div className={styles.actions}>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleDelete(vehicle.id)}
                  disabled={loading}
                >
                  Remove
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Dialog
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
        title="Add Vehicle"
        size="md"
      >
        <form onSubmit={handleAdd}>
          <div className={styles.formGrid}>
            <Input
              label="Registration Number"
              value={form.registrationNumber}
              onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
              placeholder="ABC-1234"
              required
              maxLength={32}
            />
            <Input
              label="Make"
              value={form.make}
              onChange={(e) => setForm({ ...form, make: e.target.value })}
              placeholder="Toyota"
              required
              maxLength={80}
            />
            <Input
              label="Model"
              value={form.model}
              onChange={(e) => setForm({ ...form, model: e.target.value })}
              placeholder="Hilux"
              required
              maxLength={80}
            />
            <Input
              label="Year"
              type="number"
              min={1900}
              max={2100}
              value={String(form.year)}
              onChange={(e) => setForm({ ...form, year: Number(e.target.value) })}
              required
            />
            <Input
              label="Color"
              value={form.color || ''}
              onChange={(e) => setForm({ ...form, color: e.target.value })}
              placeholder="Silver"
              maxLength={40}
            />
            <Input
              label="Chassis Number"
              value={form.chassisNumber || ''}
              onChange={(e) => setForm({ ...form, chassisNumber: e.target.value })}
              maxLength={64}
            />
            <Input
              label="Engine Number"
              value={form.engineNumber || ''}
              onChange={(e) => setForm({ ...form, engineNumber: e.target.value })}
              maxLength={64}
            />
          </div>
          <div className={styles.dialogFooter}>
            <Button variant="secondary" onClick={() => setIsAddOpen(false)} type="button">
              Cancel
            </Button>
            <Button type="submit" loading={saving}>
              Save Vehicle
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
}