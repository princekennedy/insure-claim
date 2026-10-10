'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Input, Button } from '@/lib/components/ui';
import { register, isAuthenticated } from '@/lib/services/auth';
import styles from './register.module.css';

export default function RegisterPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    fullName: '',
    phone: '',
    nic: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Redirect if already logged in
  if (typeof window !== 'undefined' && isAuthenticated()) {
    router.push('/dashboard');
    return null;
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await register(formData);
      router.push('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <h1 className={styles.title}>Create an account</h1>
      <p className={styles.subtitle}>Register to start filing claims</p>

      {error && (
        <div className={styles.error}>{error}</div>
      )}

      <Input
        label="Full name"
        name="fullName"
        value={formData.fullName}
        onChange={handleChange}
        placeholder="John Doe"
        required
      />

      <Input
        label="Email address"
        type="email"
        name="email"
        value={formData.email}
        onChange={handleChange}
        placeholder="you@example.com"
        required
        autoComplete="email"
      />

      <Input
        label="Phone number"
        type="tel"
        name="phone"
        value={formData.phone}
        onChange={handleChange}
        placeholder="+1 234 567 8900"
        hint="Optional but recommended"
      />

      <Input
        label="National ID (NIC)"
        type="text"
        name="nic"
        value={formData.nic}
        onChange={handleChange}
        placeholder="e.g., 123456789V"
        hint="Optional"
      />

      <Input
        label="Password"
        type="password"
        name="password"
        value={formData.password}
        onChange={handleChange}
        placeholder="Min 10 characters"
        required
        autoComplete="new-password"
      />

      <Button type="submit" fullWidth loading={loading}>
        Create account
      </Button>

      <p className={styles.footer}>
        Already have an account?{' '}          <Link href="/login" className={styles.link}>
          Sign in
        </Link>
      </p>

      <p className={styles.homeLink}>
        <Link href="/" className={styles.link}>
          Back to home
        </Link>
      </p>
    </form>
  );
}
