'use client';

import { useState, FormEvent, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { Input, Button } from '@/lib/components/ui';
import { resetPassword } from '@/lib/services/auth';
import styles from '../login/login.module.css';

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token') || '';

  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!token) {
      setError('Invalid or missing reset token.');
      setLoading(false);
      return;
    }

    try {
      await resetPassword({ token, newPassword: password });
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
        <h1 className={styles.title}>Password reset successful</h1>
        <p className={styles.subtitle}>You can now log in with your new password.</p>
        <p className={styles.homeLink}>
          <Link href="/login" className={styles.link}>
            Go to login
          </Link>
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <h1 className={styles.title}>Reset password</h1>
      <p className={styles.subtitle}>Enter your new password below</p>

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
        label="New Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Enter new password (min 10 chars)"
        required
      />

      <Button type="submit" fullWidth loading={loading}>
        Reset Password
      </Button>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className={styles.form}>Loading...</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
