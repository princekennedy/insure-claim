'use client';

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

export type ThemeMode = 'light' | 'dark';

interface ThemeContextValue {
  mode: ThemeMode;
  error: string;
  setMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);
const STORAGE_KEY = 'insureclaim-theme';

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setCurrentMode] = useState<ThemeMode>('light');
  const [error, setError] = useState('');

  useEffect(() => {
    try {
      const savedMode = window.localStorage.getItem(STORAGE_KEY);
      if (savedMode !== null && savedMode !== 'light' && savedMode !== 'dark') {
        throw new Error('Saved appearance preference is invalid.');
      }

      const initialMode = savedMode ?? 'light';
      document.documentElement.dataset.theme = initialMode;
      setCurrentMode(initialMode);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Saved appearance could not be loaded.');
    }
  }, []);

  function setMode(nextMode: ThemeMode) {
    try {
      window.localStorage.setItem(STORAGE_KEY, nextMode);
      document.documentElement.dataset.theme = nextMode;
      setCurrentMode(nextMode);
      setError('');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Appearance preference could not be saved.');
    }
  }

  return (
    <ThemeContext.Provider value={{ mode, error, setMode }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider.');
  }
  return context;
}
