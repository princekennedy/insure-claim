'use client';

import { ReactNode, useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Avatar } from '../ui/avatar';
import { getCurrentUser, logout } from '../../services/auth';
import styles from './app-layout.module.css';

interface CustomerLayoutProps {
  children: ReactNode;
}

function DashboardIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <rect x="3" y="3" width="7" height="9" rx="1" />
      <rect x="14" y="3" width="7" height="5" rx="1" />
      <rect x="14" y="12" width="7" height="9" rx="1" />
      <rect x="3" y="16" width="7" height="5" rx="1" />
    </svg>
  );
}

function ClaimsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
      <polyline points="10 9 9 9 8 9" />
    </svg>
  );
}

function PolicyIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
      <polyline points="10 9 9 9 8 9" />
    </svg>
  );
}

function KycIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}

function GarageIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}

function FeedbackIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  );
}

const navGroups = [
  {
    label: 'Overview',
    items: [{ href: '/customer/home', label: 'Dashboard', icon: DashboardIcon }],
  },
  {
    label: 'Claims & Policies',
    items: [
      { href: '/customer/claims', label: 'Claims', icon: ClaimsIcon },
      { href: '/customer/policies', label: 'Policies', icon: PolicyIcon },
      { href: '/customer/kyc', label: 'KYC', icon: KycIcon },
    ],
  },
  {
    label: 'Service Network',
    items: [
      { href: '/customer/garages', label: 'Garages', icon: GarageIcon },
      { href: '/customer/feedback', label: 'Feedback', icon: FeedbackIcon },
    ],
  },
];

export function CustomerLayout({ children }: CustomerLayoutProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [currentUser, setCurrentUser] = useState<{ fullName: string } | null>(null);

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((currentUser) => {
        if (!cancelled) {
          setCurrentUser({ fullName: currentUser.fullName });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCurrentUser(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogout = (e: React.MouseEvent) => {
    e.preventDefault();
    void logout().catch(() => undefined).then(() => router.replace('/'));
  };

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <Link href="/customer/home" className={styles.logo}>
            <div className={styles.logoContainer}>
              <span className={styles.logoText}>Britam</span>
              <span className={styles.logoTagline}>With you every step of the way</span>
            </div>
          </Link>
        </div>

        <nav className={styles.nav}>
          {navGroups.map((group) => (
            <div className={styles.navGroup} key={group.label}>
              <p className={styles.groupLabel}>{group.label}</p>
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`${styles.navItem} ${isActive ? styles.navItemActive : ''}`}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <Icon />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>

        <div className={styles.sidebarFooter}>
          <Link href="/login" onClick={handleLogout} className={styles.navItem}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span>Logout</span>
          </Link>
        </div>
      </aside>

      <main className={styles.main}>
        <header className={styles.header}>
          <div className={styles.headerLeft}>
            <div className={styles.searchBar}>
              <SearchIcon />
              <input
                type="text"
                placeholder="Search..."
                className={styles.searchInput}
              />
            </div>
          </div>
          <div className={styles.headerRight}>
            {pathname === '/customer/dashboard' && (
              <Link href="/customer/claims/new" className={styles.newClaimButton}>
                <PlusIcon />
                <span>New Claim</span>
              </Link>
            )}
            <button className={styles.notificationButton} aria-label="Notifications">
              <BellIcon />
              <span className={styles.notificationBadge}>3</span>
            </button>
            {currentUser && (
              <div className={styles.headerUser}>
                <Avatar name={currentUser.fullName} size="sm" />
              </div>
            )}
          </div>
        </header>
        <div className={styles.content}>{children}</div>
      </main>
    </div>
  );
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5" aria-hidden="true">
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.35-4.35" />
    </svg>
  );
}

function PlusIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5" aria-hidden="true">
      <path d="M5 12h14" />
      <path d="M12 5v14" />
    </svg>
  );
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5" aria-hidden="true">
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </svg>
  );
}
