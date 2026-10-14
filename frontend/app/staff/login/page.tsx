'use client';

import { useState, useEffect, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Input, Button, PasswordInput } from '@/lib/components/ui';
import { login, isAuthenticated } from '@/lib/services/auth';
import styles from './login.module.css';

export default function StaffLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Redirect if already logged in (middleware will handle role-based redirect)
  useEffect(() => {
    if (typeof window !== 'undefined' && isAuthenticated()) {
      // Let middleware handle the role-based redirect
      router.push('/dashboard');
    }
  }, [router]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email, password });
      // Middleware will handle role-based redirect after successful login
      router.push('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <div className={styles.header}>
        <h1 className={styles.title}>Staff Portal</h1>
        <p className={styles.subtitle}>Sign in to access the admin dashboard</p>
      </div>

      {error && (
        <div className={styles.error}>
          <svg viewBox="0 0 20 20" fill="currentColor" className={styles.errorIcon}>
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clipRule="evenodd"
            />
          </svg>
          {error}
        </div>
      )}

      <Input
        label="Email address"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="staff@britam.co.ke"
        required
        autoComplete="email"
      />

      <PasswordInput
        label="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Enter your password"
        required
        autoComplete="current-password"
        showStrength={false}
      />

      <Button type="submit" fullWidth loading={loading}>
        Sign in
      </Button>

      <div style={{ textAlign: 'center' }}>
        <Link href="/forgot-password" className={styles.link} style={{ fontSize: '0.875rem' }}>
          Forgot password?
        </Link>
      </div>
    </form>
  );
}
