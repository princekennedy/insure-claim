'use client';

import { FormEvent, KeyboardEvent, useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Alert, Button, Card, Dialog, EmptyState, Input, PageHeader, PageLoader, Pagination } from '@/lib/components/ui';
import { getUsers } from '@/lib/services/admin';
import { createCustomerVehicle, getCustomerVehicles, type CustomerVehicle } from '@/lib/services/vehicles';
import type { PageResponse, UserSummary } from '@/lib/types';
import styles from '../management.module.css';
import vehicleStyles from './vehicles.module.css';

const EMPTY_FORM = { customerId: '', registrationNumber: '', make: '', model: '', year: '', color: '', chassisNumber: '', engineNumber: '' };

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState<PageResponse<CustomerVehicle> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [customers, setCustomers] = useState<PageResponse<UserSummary> | null>(null);
  const [customerLoading, setCustomerLoading] = useState(false);
  const [customerError, setCustomerError] = useState('');
  const [customerListOpen, setCustomerListOpen] = useState(false);
  const [activeCustomer, setActiveCustomer] = useState(-1);
  const customerRequest = useRef(0);
  const customerSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const customerInput = useRef<HTMLInputElement>(null);

  useEffect(() => () => {
    customerRequest.current++;
    if (customerSearchTimer.current) clearTimeout(customerSearchTimer.current);
  }, []);

  useEffect(() => {
    if (customerListOpen && activeCustomer >= 0) {
      document.getElementById(`vehicle-customer-option-${activeCustomer}`)?.scrollIntoView({ block: 'nearest' });
    }
  }, [activeCustomer, customerListOpen]);

  const loadVehicles = useCallback(async (page: number) => {
    setLoading(true);
    setError('');
    try {
      setVehicles(await getCustomerVehicles(page));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load vehicles.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void Promise.resolve().then(() => loadVehicles(0)); }, [loadVehicles]);

  async function loadCustomers(page: number, search: string) {
    const requestId = ++customerRequest.current;
    setCustomerLoading(true);
    setCustomerError('');
    try {
      const result = await getUsers(page, 20, { role: 'CUSTOMER', query: search || undefined });
      if (requestId === customerRequest.current) setCustomers(result);
    } catch (err) {
      if (requestId === customerRequest.current) {
        setCustomers(null);
        setCustomerError(err instanceof Error ? err.message : 'Unable to load customers.');
      }
    } finally {
      if (requestId === customerRequest.current) setCustomerLoading(false);
    }
  }

  function openDialog() {
    setForm(EMPTY_FORM);
    setFormError('');
    setQuery('');
    setAppliedQuery('');
    setCustomers(null);
    setCustomerListOpen(false);
    setActiveCustomer(-1);
    setOpen(true);
    void loadCustomers(0, '');
  }

  function closeDialog() {
    if (saving) return;
    customerRequest.current++;
    if (customerSearchTimer.current) clearTimeout(customerSearchTimer.current);
    setOpen(false);
  }

  function searchCustomers(value: string) {
    const search = value.trim();
    setQuery(value);
    setAppliedQuery(search);
    setForm((current) => ({ ...current, customerId: '' }));
    setCustomerListOpen(true);
    setActiveCustomer(-1);
    setCustomers(null);
    setCustomerError('');
    setCustomerLoading(true);
    customerRequest.current++;
    if (customerSearchTimer.current) clearTimeout(customerSearchTimer.current);
    customerSearchTimer.current = setTimeout(() => void loadCustomers(0, search), 250);
  }

  function selectCustomer(customer: UserSummary) {
    setForm((current) => ({ ...current, customerId: String(customer.id) }));
    setQuery(`${customer.fullName} (${customer.email})`);
    customerInput.current?.focus();
    setCustomerListOpen(false);
    setActiveCustomer(-1);
  }

  function handleCustomerKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    const results = customers?.content ?? [];
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      setCustomerListOpen(true);
      if (results.length) {
        setActiveCustomer((current) => event.key === 'ArrowDown'
          ? Math.min(current + 1, results.length - 1)
          : current <= 0 ? results.length - 1 : current - 1);
      }
    } else if (event.key === 'Enter' && customerListOpen) {
      event.preventDefault();
      if (results[activeCustomer]) selectCustomer(results[activeCustomer]);
    } else if (event.key === 'Escape' && customerListOpen) {
      event.preventDefault();
      event.stopPropagation();
      setCustomerListOpen(false);
      setActiveCustomer(-1);
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const year = Number(form.year);
    if (!form.customerId || !form.registrationNumber.trim() || !form.make.trim() || !form.model.trim() ||
      !form.year || !Number.isInteger(year) || year < 1900 || year > 2100) {
      setFormError('Select a customer and enter a registration number, make, model and year between 1900 and 2100.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const created = await createCustomerVehicle(Number(form.customerId), {
        registrationNumber: form.registrationNumber.trim(),
        make: form.make.trim(),
        model: form.model.trim(),
        year,
        color: form.color.trim() || undefined,
        chassisNumber: form.chassisNumber.trim() || undefined,
        engineNumber: form.engineNumber.trim() || undefined,
      });
      setOpen(false);
      setNotice(`${created.registrationNumber} was added successfully.`);
      await loadVehicles(0);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Unable to add the vehicle.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className={styles.page}>
      <PageHeader title="Vehicles" description="View customer vehicles and register a vehicle to a customer account."
        actions={<Button type="button" onClick={openDialog}>Add vehicle</Button>} />
      {notice && <div role="status"><Alert type="success">{notice}</Alert></div>}
      {error && <Alert type="error">{error}</Alert>}
      {loading && !vehicles ? <PageLoader message="Loading vehicles..." /> : vehicles && (
        <>
          <p className={styles.resultLine}>{vehicles.totalElements} {vehicles.totalElements === 1 ? 'vehicle' : 'vehicles'} in directory</p>
          {vehicles.empty ? <EmptyState title="No vehicles found" description="Add a vehicle to a customer account to get started." /> : (
            <Card className={styles.tableCard} padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Customer vehicles</caption>
                  <thead><tr><th scope="col">Registration</th><th scope="col">Vehicle</th><th scope="col">Customer</th></tr></thead>
                  <tbody>{vehicles.content.map(({ vehicle, customerId, customerName, customerEmail }) => (
                    <tr key={vehicle.id}>
                      <td className={styles.primaryCell}>{vehicle.registrationNumber}</td>
                      <td>{vehicle.year} {vehicle.make} {vehicle.model}</td>
                      <td><Link className={styles.customerLink} href={`/customers/${customerId}`}>{customerName}</Link><span className={styles.secondaryText}>{customerEmail}</span></td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            </Card>
          )}
          <Pagination page={vehicles.page} totalPages={vehicles.totalPages} label="Vehicle directory pages" loading={loading} onPageChange={(page) => void loadVehicles(page)} />
        </>
      )}
      {error && !vehicles && <Button type="button" variant="secondary" onClick={() => void loadVehicles(0)}>Try again</Button>}

      <Dialog isOpen={open} onClose={closeDialog} title="Add vehicle" size="lg" closeOnOverlay={!saving} showCloseButton={!saving}
        footer={<><Button type="button" variant="secondary" disabled={saving} onClick={closeDialog}>Cancel</Button><Button type="submit" form="add-vehicle-form" loading={saving}>Add vehicle</Button></>}>
        <form id="add-vehicle-form" className={`${styles.dialogForm} ${vehicleStyles.form}`} onSubmit={handleCreate} noValidate>
          <p className={vehicleStyles.intro}>Choose the customer who owns this vehicle, then enter its registration details.</p>
          {formError && <div role="alert"><Alert type="error">{formError}</Alert></div>}
          <div className={vehicleStyles.customerPicker} onBlur={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) {
              setCustomerListOpen(false);
              setActiveCustomer(-1);
            }
          }}>
            <Input ref={customerInput} label="Customer" required aria-required="true" role="combobox" aria-autocomplete="list"
              aria-expanded={customerListOpen} aria-controls={customerListOpen ? 'vehicle-customer-options' : undefined}
              aria-activedescendant={customerListOpen && activeCustomer >= 0 ? `vehicle-customer-option-${activeCustomer}` : undefined}
              value={query} onChange={(event) => searchCustomers(event.target.value)}
              onFocus={() => setCustomerListOpen(true)} onKeyDown={handleCustomerKeyDown}
              placeholder="Search name or email" disabled={saving} autoComplete="off" />
            {customerListOpen && (
              <>
                {customerLoading && <p className={vehicleStyles.help} role="status">Loading customers...</p>}
                {customerError && <div role="alert"><Alert type="error">{customerError} <button type="button" onClick={() => void loadCustomers(0, appliedQuery)}>Retry</button></Alert></div>}
                {!customerLoading && customers?.empty && <p className={vehicleStyles.help} role="status">No customers found. Try a different search.</p>}
                <ul id="vehicle-customer-options" role="listbox" aria-label="Customers" className={vehicleStyles.customerOptions}>
                  {!customerLoading && customers?.content.map((customer, index) => (
                    <li id={`vehicle-customer-option-${index}`} role="option" aria-selected={form.customerId === String(customer.id)}
                      className={index === activeCustomer ? vehicleStyles.activeOption : undefined} key={customer.id}
                      onMouseDown={(event) => event.preventDefault()} onClick={() => selectCustomer(customer)}>
                      {customer.fullName} ({customer.email})
                    </li>
                  ))}
                </ul>
                {customers && <Pagination page={customers.page} totalPages={customers.totalPages} label="Customer search pages" loading={customerLoading || saving}
                  onPageChange={(page) => { setActiveCustomer(-1); void loadCustomers(page, appliedQuery); }} />}
              </>
            )}
          </div>
          <div className={vehicleStyles.grid}>
            <Input label="Registration number" required aria-required="true" maxLength={32} value={form.registrationNumber} disabled={saving} onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })} />
            <Input label="Year" required aria-required="true" type="number" min={1900} max={2100} step={1} value={form.year} disabled={saving} onChange={(e) => setForm({ ...form, year: e.target.value })} />
            <Input label="Make" required aria-required="true" maxLength={80} value={form.make} disabled={saving} onChange={(e) => setForm({ ...form, make: e.target.value })} />
            <Input label="Model" required aria-required="true" maxLength={80} value={form.model} disabled={saving} onChange={(e) => setForm({ ...form, model: e.target.value })} />
            <Input label="Color" maxLength={40} value={form.color} disabled={saving} onChange={(e) => setForm({ ...form, color: e.target.value })} />
            <Input label="Chassis number" maxLength={64} value={form.chassisNumber} disabled={saving} onChange={(e) => setForm({ ...form, chassisNumber: e.target.value })} />
            <Input label="Engine number" maxLength={64} value={form.engineNumber} disabled={saving} onChange={(e) => setForm({ ...form, engineNumber: e.target.value })} />
          </div>
        </form>
      </Dialog>
    </section>
  );
}
