'use client';

import { useState, forwardRef, InputHTMLAttributes } from 'react';
import styles from './phone-input.module.css';

interface Country {
  code: string;
  dial: string;
  flag: string;
}

const countries: Country[] = [
  { code: 'MW', dial: '+265', flag: 'MW' },
];

interface PhoneInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'value'> {
  label?: string;
  error?: string;
  hint?: string;
  required?: boolean;
  value?: string;
  onValueChange?: (value: string) => void;
  defaultCountry?: string;
}

export const PhoneInput = forwardRef<HTMLInputElement, PhoneInputProps>(
  ({ label, error, hint, required, value = '', onValueChange, defaultCountry = 'MW', className = '', id, ...props }, ref) => {
    const [selectedCountry] = useState(
      countries.find((c) => c.code === defaultCountry) || countries[0]
    );
    const inputId = id || `phone-${Math.random().toString(36).slice(2, 9)}`;

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value.replace(/[^0-9]/g, '');
      onValueChange?.(newValue);
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
          <div className={styles.countrySelector}>
            <div className={styles.countryButton}>
              <span className={styles.flag}>{selectedCountry.flag}</span>
              <span className={styles.dialCode}>{selectedCountry.dial}</span>
            </div>
          </div>
          <input
            ref={ref}
            id={inputId}
            type="tel"
            className={`${styles.input} ${error ? styles.error : ''} ${className}`}
            value={value}
            onChange={handleChange}
            {...props}
          />
        </div>
        {error && <p className={styles.errorMessage}>{error}</p>}
        {hint && !error && <p className={styles.hint}>{hint}</p>}
      </div>
    );
  },
);

PhoneInput.displayName = 'PhoneInput';
