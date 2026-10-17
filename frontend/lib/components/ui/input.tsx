'use client';

import { InputHTMLAttributes, forwardRef, useId } from 'react';
import styles from './input.module.css';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  required?: boolean;
  success?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, required, success, className = '', id, ...props }, ref) => {
    const generatedId = useId();
    const inputId = id ?? `input-${generatedId}`;

    return (
      <div className={styles.wrapper}>
        {label && (
          <label htmlFor={inputId} className={styles.label}>
            {label}
            {required && <span className={styles.asterisk}> *</span>}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          className={`${styles.input} ${error ? styles.error : ''} ${success ? styles.success : ''} ${className}`}
          {...props}
        />
        {error && <p className={styles.errorMessage}>{error}</p>}
        {hint && !error && <p className={styles.hint}>{hint}</p>}
      </div>
    );
  },
);

Input.displayName = 'Input';
