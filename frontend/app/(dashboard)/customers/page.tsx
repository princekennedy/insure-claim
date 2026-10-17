'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
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
} from '@/lib/components/ui';
import {
  createUser,
  getUsers,
} from '@/lib/services/admin';
import { getAdminPolicies } from '@/lib/services/policies';
import type {
  PageResponse,
  UserSummary,
} from '@/lib/types';
import type { PolicyAdminResponse } from '@/lib/types/policy';
import styles from '../management.module.css';

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

const EMPTY_CREATE_FORM: CreateCustomerFormState = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  nic: '',
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
                          <Link className={styles.customerLink} href={`/customers/${customer.id}`}>
                            {customer.fullName}
                          </Link>
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
        <form id="create-customer-form" className={styles.dialogForm} onSubmit={handleCreate}>
          {createError && <Alert type="error">{createError}</Alert>}
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
            label="NIC"
            value={createForm.nic}
            onChange={(event) => setCreateForm((prev) => ({ ...prev, nic: event.target.value }))}
            maxLength={32}
          />
        </form>
      </Dialog>

    </section>
  );
}