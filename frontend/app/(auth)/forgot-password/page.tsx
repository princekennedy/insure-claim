'use client';

import { useState, FormEvent } from 'react';
import Link from 'next/link';
import { Input, Button } from '@/lib/components/ui';
import { forgotPassword } from '@/lib/services/auth';
import styles from '../login/login.module.css';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await forgotPassword({ email });
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className={styles.form} style={{ textAlign: 'center' }}>
        <h1 className={styles.title}>Check your email</h1>
        <p className={styles.subtitle}>
          If an account exists with {email}, we've sent instructions to reset your password.
          (For this demo, since emails are not sent, simply go to <Link href="/reset-password?token=YOUR_TOKEN" className={styles.link}>Reset Password</Link> assuming you extracted the token from the backend logs).
        </p>
        <p className={styles.homeLink}>
          <Link href="/login" className={styles.link}>
            Back to login
          </Link>
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <h1 className={styles.title}>Forgot password</h1>
      <p className={styles.subtitle}>Enter your email to receive a password reset link</p>

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
        placeholder="you@example.com"
        required
        autoComplete="email"
      />

      <Button type="submit" fullWidth loading={loading}>
        Send reset link
      </Button>

      <p className={styles.homeLink}>
        <Link href="/login" className={styles.link}>
          Back to login
        </Link>
      </p>
    </form>
  );
}
