'use client';

import { useEffect, useRef, useState } from 'react';
import { usePathname } from 'next/navigation';
import styles from './login-progress-bar.module.css';

const LOGIN_TRANSITION_EVENT = 'insure-claim:login-transition';
const VISIBLE_DURATION = 1500;
const MAX_TRANSITION_DURATION = 15000;

export function startLoginTransition() {
  window.dispatchEvent(new Event(LOGIN_TRANSITION_EVENT));
}

export function LoginTransitionProgress() {
  const pathname = usePathname();
  const [active, setActive] = useState(false);
  const [transitionId, setTransitionId] = useState(0);
  const fallbackTimer = useRef<number | null>(null);

  useEffect(() => {
    function handleLoginTransition() {
      setActive(true);
      setTransitionId((id) => id + 1);

      if (fallbackTimer.current !== null) {
        window.clearTimeout(fallbackTimer.current);
      }
      fallbackTimer.current = window.setTimeout(() => {
        setActive(false);
        fallbackTimer.current = null;
      }, MAX_TRANSITION_DURATION);
    }

    window.addEventListener(LOGIN_TRANSITION_EVENT, handleLoginTransition);
    return () => {
      window.removeEventListener(LOGIN_TRANSITION_EVENT, handleLoginTransition);
      if (fallbackTimer.current !== null) {
        window.clearTimeout(fallbackTimer.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!active || (pathname !== '/dashboard' && pathname !== '/customer/dashboard')) {
      return;
    }

    if (fallbackTimer.current !== null) {
      window.clearTimeout(fallbackTimer.current);
      fallbackTimer.current = null;
    }

    const timer = window.setTimeout(() => {
      setActive(false);
    }, VISIBLE_DURATION);

    return () => window.clearTimeout(timer);
  }, [active, pathname]);

  if (!active || (pathname !== '/dashboard' && pathname !== '/customer/dashboard')) {
    return null;
  }

  return (
    <div className={styles.track} role="progressbar" aria-label="Opening dashboard">
      <div className={styles.fill} key={transitionId} />
    </div>
  );
}
