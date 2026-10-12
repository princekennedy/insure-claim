'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Input, Button, PasswordInput, PhoneInput } from '@/lib/components/ui';
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
  const [phoneValue, setPhoneValue] = useState('');
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

      <PhoneInput
        label="Phone number"
        value={phoneValue}
        onValueChange={(value) => {
          setPhoneValue(value);
          setFormData((prev) => ({ ...prev, phone: value }));
        }}
        placeholder="712345678"
        required
        defaultCountry="MW"
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

      <PasswordInput
        label="Password"
        name="password"
        value={formData.password}
        onChange={handleChange}
        placeholder="Min 10 characters"
        required
        autoComplete="new-password"
        showStrength
      />

      <div className={styles.securityNote}>
        <svg viewBox="0 0 20 20" fill="currentColor" className={styles.lockIcon}>
          <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
        </svg>
        <span>Secure registration with end-to-end encryption</span>
      </div>

      <Button type="submit" fullWidth loading={loading}>
        Create account
      </Button>

      <p className={styles.privacyNote}>
        By creating an account, you agree to our{' '}
        <Link href="/terms" className={styles.link}>
          Terms of Service
        </Link>{' '}
        and{' '}
        <Link href="/privacy" className={styles.link}>
          Privacy Policy
        </Link>
      </p>

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
