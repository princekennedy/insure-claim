'use client';

import { useState, FormEvent } from 'react';
import { Dialog } from './dialog';
import { Input, Button, PasswordInput } from './index';
import styles from './staff-login-modal.module.css';

interface StaffLoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onLogin: (email: string, password: string) => Promise<void>;
  loading?: boolean;
  error?: string;
}

export function StaffLoginModal({
  isOpen,
  onClose,
  onLogin,
  loading = false,
  error = '',
}: StaffLoginModalProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    await onLogin(email, password);
  }

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title="Staff Login"
      size="sm"
      closeOnOverlay={false}
    >
      <form onSubmit={handleSubmit} className={styles.form}>
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
          Sign In
        </Button>
      </form>
    </Dialog>
  );
}
