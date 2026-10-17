'use client';

import { useId, useState, forwardRef, InputHTMLAttributes } from 'react';
import styles from './password-input.module.css';

interface PasswordInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  required?: boolean;
  showStrength?: boolean;
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ label, error, hint, required, showStrength = true, className = '', id, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);
    const [strength, setStrength] = useState(0);
    const generatedId = useId();
    const inputId = id ?? `password-${generatedId}`;

    const calculateStrength = (password: string) => {
      let score = 0;
      if (password.length >= 10) score++;
      if (password.length >= 12) score++;
      if (/[A-Z]/.test(password)) score++;
      if (/[a-z]/.test(password)) score++;
      if (/[0-9]/.test(password)) score++;
      if (/[^A-Za-z0-9]/.test(password)) score++;
      return score;
    };

    const getStrengthLabel = (score: number) => {
      if (score <= 1) return 'Weak';
      if (score <= 3) return 'Fair';
      if (score <= 4) return 'Good';
      return 'Strong';
    };

    const getStrengthColor = (score: number) => {
      if (score <= 1) return '#dc2626';
      if (score <= 3) return '#f59e0b';
      if (score <= 4) return '#10b981';
      return '#059669';
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (showStrength) {
        setStrength(calculateStrength(e.target.value));
      }
      props.onChange?.(e);
    };

    return (
      <div className={styles.wrapper}>
        {label && (
          <label htmlFor={inputId} className={styles.label}>
            {label}
            {required && <span className={styles.asterisk}> *</span>}
          </label>
        )}
        <div className={styles.inputContainer}>
          <input
            ref={ref}
            id={inputId}
            type={showPassword ? 'text' : 'password'}
            className={`${styles.input} ${error ? styles.error : ''} ${className}`}
            onChange={handleChange}
            {...props}
          />
          <button
            type="button"
            className={styles.toggle}
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? (
              <svg viewBox="0 0 20 20" fill="currentColor" className={styles.icon}>
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
              </svg>
            ) : (
              <svg viewBox="0 0 20 20" fill="currentColor" className={styles.icon}>
                <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.064 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            )}
          </button>
        </div>
        {showStrength && props.value && (
          <div className={styles.strength}>
            <div className={styles.strengthBar}>
              <div
                className={styles.strengthFill}
                style={{
                  width: `${(strength / 6) * 100}%`,
                  backgroundColor: getStrengthColor(strength),
                }}
              />
            </div>
            <span className={styles.strengthText} style={{ color: getStrengthColor(strength) }}>
              {getStrengthLabel(strength)}
            </span>
          </div>
        )}
        {error && <p className={styles.errorMessage}>{error}</p>}
        {hint && !error && <p className={styles.hint}>{hint}</p>}
      </div>
    );
  },
);

PasswordInput.displayName = 'PasswordInput';
