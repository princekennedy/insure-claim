'use client';

import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
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
  Pagination,
  Select,
} from '@/lib/components/ui';
import {
  createUser,
  getUsers,
} from '@/lib/services/admin';
import { createPolicy, getAdminPolicies } from '@/lib/services/policies';
import { getCustomerVehicles } from '@/lib/services/vehicles';
import { oneYearAfter } from '@/lib/policy-dates';
import { formatPremiumInput } from '@/lib/premium-amount';
import type {
  PageResponse,
  UserSummary,
} from '@/lib/types';
import type { PolicyAdminResponse } from '@/lib/types/policy';
import type { VehicleResponse } from '@/lib/types/vehicle';
import styles from '../management.module.css';
import customerStyles from './customers.module.css';

const PAGE_SIZE = 20;

interface CustomerFilters {
  query: string;
}

interface CreateCustomerFormState {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  nic: string;
}

interface CreatePolicyFormState {
  vehicleId: string;
  policyNumber: string;
  startDate: string;
  endDate: string;
  premiumAmount: string;
  productCode: string;
}

const EMPTY_CREATE_FORM: CreateCustomerFormState = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  nic: '',
};

const EMPTY_POLICY_FORM: CreatePolicyFormState = {
  vehicleId: '',
  policyNumber: '',
  startDate: '',
  endDate: '',
  premiumAmount: '',
  productCode: '',
};

export default function CustomersPage() {
  const [result, setResult] = useState<PageResponse<UserSummary> | null>(null);
  const [query, setQuery] = useState('');
  const [appliedFilters, setAppliedFilters] = useState<CustomerFilters>({ query: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateCustomerFormState>(EMPTY_CREATE_FORM);
  const [createError, setCreateError] = useState('');
  const [saving, setSaving] = useState(false);

  const [customerPolicies, setCustomerPolicies] = useState<Record<number, PolicyAdminResponse[]>>({});
  const [policyCustomer, setPolicyCustomer] = useState<UserSummary | null>(null);
  const [policyOpen, setPolicyOpen] = useState(false);
  const [policyForm, setPolicyForm] = useState<CreatePolicyFormState>(EMPTY_POLICY_FORM);
  const [policyError, setPolicyError] = useState('');
  const [policySaving, setPolicySaving] = useState(false);
  const [policyVehicles, setPolicyVehicles] = useState<VehicleResponse[]>([]);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);
  const [vehiclesError, setVehiclesError] = useState('');
  const [notice, setNotice] = useState('');
  const vehicleRequest = useRef(0);

  const loadCustomers = useCallback(async (page: number, filters: CustomerFilters) => {
    setLoading(true);
    setError('');
    try {
      const data = await getUsers(page, PAGE_SIZE, {
        query: filters.query || undefined,
        role: 'CUSTOMER',
      });
      setResult(data);
      // Load policies for each customer
      const policyPromises = data.content.map(async (customer) => {
        try {
          const policiesData = await getAdminPolicies(0, 10, { customerId: customer.id });
          return { customerId: customer.id, policies: policiesData.content };
        } catch {
          return { customerId: customer.id, policies: [] };
        }
      });
      const policyResults = await Promise.all(policyPromises);
      const policiesMap: Record<number, PolicyAdminResponse[]> = {};
      policyResults.forEach(({ customerId, policies }) => {
        policiesMap[customerId] = policies;
      });
      setCustomerPolicies(policiesMap);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load customers.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(() => loadCustomers(0, appliedFilters));
  }, [appliedFilters, loadCustomers]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAppliedFilters({ query: query.trim() });
  }

  function clearFilters() {
    setQuery('');
    setAppliedFilters({ query: '' });
  }

  async function loadCustomerVehicles(customerId: number) {
    const requestId = ++vehicleRequest.current;
    setVehiclesLoading(true);
    setVehiclesError('');
    setPolicyVehicles([]);
    try {
      const firstPage = await getCustomerVehicles(0);
      const remainingPages = await Promise.all(
        Array.from(
          { length: Math.max(0, firstPage.totalPages - 1) },
          (_, index) => getCustomerVehicles(index + 1),
        ),
      );
      if (requestId !== vehicleRequest.current) return;
      setPolicyVehicles(
        [firstPage, ...remainingPages]
          .flatMap((page) => page.content)
          .filter((entry) => entry.customerId === customerId)
          .map((entry) => entry.vehicle),
      );
    } catch (err) {
      if (requestId === vehicleRequest.current) {
        setVehiclesError(err instanceof Error ? err.message : 'Unable to load this customer’s vehicles.');
      }
    } finally {
      if (requestId === vehicleRequest.current) setVehiclesLoading(false);
    }
  }

  function openPolicyDialog(customer: UserSummary) {
    setPolicyCustomer(customer);
    setPolicyForm(EMPTY_POLICY_FORM);
    setPolicyError('');
    setVehiclesError('');
    setPolicyOpen(true);
    void loadCustomerVehicles(customer.id);
  }

  function closePolicyDialog() {
    if (policySaving) return;
    vehicleRequest.current++;
    setPolicyOpen(false);
    setPolicyCustomer(null);
  }

  async function handleCreatePolicy(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!policyCustomer) return;
    const premiumAmount = Number(policyForm.premiumAmount);
    const today = new Date();
    const todayInput = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    if (!policyForm.vehicleId || !policyForm.policyNumber.trim() || !policyForm.productCode ||
      !policyForm.startDate || !policyForm.endDate ||
      !/^\d{1,10}(\.\d{1,2})?$/.test(policyForm.premiumAmount) ||
      !Number.isFinite(premiumAmount) || premiumAmount <= 0) {
      setPolicyError('Select a vehicle and insurance type, and enter the policy number, dates, and a valid premium.');
      return;
    }
    if (!oneYearAfter(policyForm.startDate) || policyForm.endDate !== oneYearAfter(policyForm.startDate)) {
      setPolicyError('The end date must be one year after the start date.');
      return;
    }
    if (policyForm.endDate < todayInput) {
      setPolicyError('The end date cannot be in the past.');
      return;
    }

    setPolicySaving(true);
    setPolicyError('');
    try {
      const created = await createPolicy({
        customerId: policyCustomer.id,
        vehicleId: Number(policyForm.vehicleId),
        policyNumber: policyForm.policyNumber.trim(),
        startDate: policyForm.startDate,
        endDate: policyForm.endDate,
        premiumAmount,
        productCode: policyForm.productCode,
      });
      setCustomerPolicies((previous) => ({
        ...previous,
        [policyCustomer.id]: [...(previous[policyCustomer.id] ?? []), created],
      }));
      setNotice(`Policy ${created.policyNumber} was added for ${policyCustomer.fullName}.`);
      setPolicyOpen(false);
      setPolicyCustomer(null);
    } catch (err) {
      setPolicyError(err instanceof Error ? err.message : 'The policy could not be created.');
    } finally {
      setPolicySaving(false);
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (
      !createForm.email.trim() ||
      !createForm.password ||
      !createForm.fullName.trim()
    ) {
      setCreateError('Email, password, and full name are required.');
      return;
    }
    setSaving(true);
    setCreateError('');
    try {
      await createUser({
        email: createForm.email.trim(),
        password: createForm.password,
        fullName: createForm.fullName.trim(),
        phone: createForm.phone.trim() || undefined,
        nic: createForm.nic.trim() || undefined,
        role: 'CUSTOMER',
      });
      setCreateOpen(false);
      await loadCustomers(0, appliedFilters);
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'The customer could not be created.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className={styles.page}>
      <PageHeader
        title="Customers"
        description="Manage customer accounts for Britam Insurance. View, edit, enable/disable, and create customer profiles."
        actions={
          <Button type="button" onClick={() => setCreateOpen(true)}>
            New customer
          </Button>
        }
      />

      {notice && <div role="status"><Alert type="success" dismissible onDismiss={() => setNotice('')}>{notice}</Alert></div>}

      <form className={styles.filterBar} onSubmit={handleSearch} aria-label="Filter customers">
        <div className={styles.filterField}>
          <label className={styles.filterLabel} htmlFor="customer-search">Search customers</label>
          <input
            className={styles.filterInput}
            id="customer-search"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by name or email"
          />
        </div>
        <div className={styles.filterActions}>
          <Button type="submit" loading={loading}>Apply</Button>
          <Button type="button" variant="secondary" onClick={clearFilters}>Clear</Button>
        </div>
      </form>

      {error && (
        <Alert type="error">{error}</Alert>
      )}

      {loading && !result ? (
        <PageLoader message="Loading customers..." />
      ) : result ? (
        <>
          <p className={styles.resultLine}>
            {result.totalElements} {result.totalElements === 1 ? 'customer' : 'customers'}
            {appliedFilters.query ? ' match these filters' : ' in directory'}
          </p>
          {result.empty ? (
            <EmptyState
              title="No customers found"
              description="No customer accounts match the selected search filters."
            />
          ) : (
            <Card className={styles.tableCard} padding="none">
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <caption className="sr-only">Customer directory</caption>
                  <thead>
                    <tr>
                      <th scope="col">Name</th>
                      <th scope="col">Email</th>
                      <th scope="col">Policies</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.content.map((customer) => (
                      <tr key={customer.id}>
                        <td className={styles.primaryCell}>
                          <div className={customerStyles.customerNameActions}>
                            <Link className={styles.customerLink} href={`/customers/${customer.id}`}>
                              {customer.fullName}
                            </Link>
                            <Button
                              type="button"
                              variant="primary"
                              size="sm"
                              onClick={() => openPolicyDialog(customer)}
                              aria-label={`Add policy for ${customer.fullName}`}
                            >
                              Add policy
                            </Button>
                          </div>
                        </td>
                        <td>{customer.email}</td>
                        <td>
                          {customerPolicies[customer.id]?.length ? (
                            <div className={styles.policyTags}>
                              {Array.from(new Set(customerPolicies[customer.id].map(p => p.productCode))).map((productCode) => {
                                const policy = customerPolicies[customer.id].find(p => p.productCode === productCode);
                                return (
                                  <Badge key={productCode} variant={policy?.isCurrentlyValid ? 'success' : 'warning'} size="sm">
                                    {productCode}
                                  </Badge>
                                );
                              })}
                            </div>
                          ) : (
                            <span className={styles.noPolicies}>No policies</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            label="Customer directory pages"
            loading={loading}
            onPageChange={(next) => void loadCustomers(next, appliedFilters)}
          />
        </>
      ) : null}

      <Dialog
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        title="New customer"
        size="lg"
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="create-customer-form" loading={saving}>
              Create
            </Button>
          </>
        }
      >
        <form id="create-customer-form" className={`${styles.dialogForm} ${customerStyles.customerForm}`} onSubmit={handleCreate}>
          <p className={customerStyles.customerIntro}>Enter the customer&apos;s account and contact details.</p>
          {createError && <Alert type="error">{createError}</Alert>}
          <div className={customerStyles.customerGrid}>
            <Input
              label="Full name"
              value={createForm.fullName}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, fullName: event.target.value }))}
              maxLength={160}
              required
            />
            <Input
              label="Email"
              type="email"
              value={createForm.email}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, email: event.target.value }))}
              required
            />
            <Input
              label="Password"
              type="password"
              value={createForm.password}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, password: event.target.value }))}
              hint="At least 10 characters with upper, lower, digit and symbol"
              autoComplete="new-password"
              required
            />
            <Input
              label="Phone"
              value={createForm.phone}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, phone: event.target.value }))}
              placeholder="+254 712 345 678"
            />
            <Input
              label="National Identity Card"
              value={createForm.nic}
              onChange={(event) => setCreateForm((prev) => ({ ...prev, nic: event.target.value }))}
              maxLength={32}
            />
          </div>
        </form>
      </Dialog>

      <Dialog
        isOpen={policyOpen}
        onClose={closePolicyDialog}
        title="Add policy"
        size="lg"
        closeOnOverlay={!policySaving}
        showCloseButton={!policySaving}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={closePolicyDialog} disabled={policySaving}>
              Cancel
            </Button>
            <Button
              type="submit"
              form="create-policy-form"
              loading={policySaving}
              disabled={vehiclesLoading || !!vehiclesError || policyVehicles.length === 0}
            >
              Create policy
            </Button>
          </>
        }
      >
        <form id="create-policy-form" className={`${styles.dialogForm} ${customerStyles.policyForm}`} onSubmit={handleCreatePolicy} noValidate>
          {policyCustomer && (
            <p className={customerStyles.policyIntro}>
              Create cover for <strong>{policyCustomer.fullName}</strong> · {policyCustomer.email}
            </p>
          )}
          {policyError && <div role="alert"><Alert type="error">{policyError}</Alert></div>}
          {vehiclesError && (
            <div role="alert">
              <Alert type="error">
                {vehiclesError}{' '}
                <button type="button" onClick={() => policyCustomer && void loadCustomerVehicles(policyCustomer.id)} disabled={vehiclesLoading || policySaving}>
                  Retry
                </button>
              </Alert>
            </div>
          )}
          <Select
            label="Vehicle model and registration number *"
            value={policyForm.vehicleId}
            onChange={(event) => setPolicyForm((previous) => ({ ...previous, vehicleId: event.target.value }))}
            options={policyVehicles.map((vehicle) => ({
              value: String(vehicle.id),
              label: `${vehicle.registrationNumber} · ${vehicle.year} ${vehicle.make} ${vehicle.model}`,
            }))}
            placeholder={vehiclesLoading ? 'Loading customer vehicles…' : 'Select a vehicle'}
            disabled={vehiclesLoading || !!vehiclesError || policyVehicles.length === 0 || policySaving}
            required
          />
          {!vehiclesLoading && !vehiclesError && policyVehicles.length === 0 && (
            <p className={customerStyles.policyHelp}>This customer has no registered vehicles to insure. Add one on the <Link href="/vehicles">vehicles page</Link> first.</p>
          )}
          <div className={customerStyles.policyGrid}>
            <Input
              label="Start date"
              type="date"
              max="9998-12-31"
              value={policyForm.startDate}
              onChange={(event) => setPolicyForm((previous) => ({
                ...previous,
                startDate: event.target.value,
                endDate: event.target.value ? oneYearAfter(event.target.value) : '',
              }))}
              required
              disabled={policySaving}
            />
            <Input
              label="End date"
              type="date"
              value={policyForm.endDate}
              onChange={(event) => setPolicyForm((previous) => ({ ...previous, endDate: event.target.value }))}
              required
              disabled={policySaving}
            />
            <Select
              label="Vehicle insurance type *"
              value={policyForm.productCode}
              onChange={(event) => setPolicyForm((previous) => ({ ...previous, productCode: event.target.value }))}
              options={[
                { value: 'MOTOR_THIRD_PARTY', label: 'Third party' },
                { value: 'MOTOR_COMPREHENSIVE', label: 'Comprehensive' },
              ]}
              placeholder="Select insurance type"
              required
              disabled={policySaving}
            />
            <div className={customerStyles.premiumField}>
              <Input
                label="Premium amount"
                aria-label="Premium amount in Malawian kwacha"
                aria-required="true"
                type="text"
                inputMode="decimal"
                className={customerStyles.premiumInput}
                value={formatPremiumInput(policyForm.premiumAmount, 0).formatted}
                onChange={(event) => {
                  const input = event.currentTarget;
                  const { raw, formatted, cursor } = formatPremiumInput(input.value, input.selectionStart ?? input.value.length);
                  input.value = formatted;
                  input.setSelectionRange(cursor, cursor);
                  setPolicyForm((previous) => ({ ...previous, premiumAmount: raw }));
                }}
                required
                disabled={policySaving}
              />
              <span className={customerStyles.premiumPrefix} aria-hidden="true">MK</span>
            </div>
            <Input
              label="Policy number"
              maxLength={48}
              value={policyForm.policyNumber}
              onChange={(event) => setPolicyForm((previous) => ({ ...previous, policyNumber: event.target.value }))}
              required
              disabled={policySaving}
            />
          </div>
        </form>
      </Dialog>

    </section>
  );
}