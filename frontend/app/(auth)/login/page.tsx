'use client';

import { useState, useEffect, useSyncExternalStore, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Input, Button, PasswordInput } from '@/lib/components/ui';
import { login, isAuthenticated } from '@/lib/services/auth';
import { clearTokens } from '@/lib/services/api';
import styles from './login.module.css';

// Subscribing to the query string keeps the server and first client render
// identical (empty), so the "blocked" note only appears after hydration.
function useLocationSearch(): string {
  return useSyncExternalStore(
    (onChange) => {
      window.addEventListener('popstate', onChange);
      return () => window.removeEventListener('popstate', onChange);
    },
    () => window.location.search,
    () => '',
  );
}

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const blocked = new URLSearchParams(useLocationSearch()).get('blocked') === '1';

  // Two directions bounce between /login and /dashboard:
  //   * this page sends us to the portal whenever a session token exists,
  //   * the guards (middleware + the server layout) send us back to /login
  //     whenever they reject it.
  // A guard that rejects but leaves the tokens behind would therefore bounce
  // forever, so every rejection is tagged with `?blocked=1`. Seeing that tag
  // means "the last attempt was turned away" - drop the tokens and show the
  // form instead of asking for the portal again.
  useEffect(() => {
    if (typeof window === 'undefined') return;

    if (blocked) {
      clearTokens();
      return;
    }

    const hasSessionCookie = /(?:^|;\s*)accessToken=/.test(document.cookie);
    if (hasSessionCookie && isAuthenticated()) {
      router.replace('/dashboard');
    } else if (!hasSessionCookie) {
      clearTokens();
    }
  }, [router, blocked]);

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
      <h1 className={styles.title}>Welcome back</h1>

      {(error || blocked) && (
        <div className={styles.error}>
          <svg viewBox="0 0 20 20" fill="currentColor" className={styles.errorIcon}>
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clipRule="evenodd"
            />
          </svg>
          {error || 'Your session expired. Please sign in again.'}
        </div>
      )}

      <Input
        label="Email address"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
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

      <div className={styles.securityNote}>
        <svg viewBox="0 0 20 20" fill="currentColor" className={styles.lockIcon}>
          <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
        </svg>
        <span>Secure login with end-to-end encryption</span>
      </div>

      <Button type="submit" fullWidth loading={loading}>
        Sign in
      </Button>

      <div style={{ textAlign: 'center' }}>
        <Link href="/forgot-password" className={styles.link} style={{ fontSize: '0.875rem' }}>
          Forgot password?
        </Link>
      </div>

      <p className={styles.footer}>
        Don&apos;t have an account?{' '}          <Link href="/register" className={styles.link}>
          Sign up
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
