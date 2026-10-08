'use client';

import { ReactNode, useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Avatar } from '../ui/avatar';
import { getCurrentUser, logout } from '../../services/auth';
import type { Role } from '../../types/auth';
import styles from './app-layout.module.css';

interface AppLayoutProps {
  children: ReactNode;
}

const navGroups = [
  {
    label: 'Overview',
    items: [{ href: '/dashboard', label: 'Dashboard', icon: DashboardIcon }],
  },
  {
    label: 'Claims & Policies',
    items: [
      { href: '/claims', label: 'Claims', icon: ClaimsIcon },
      { href: '/vehicles', label: 'Vehicles', icon: VehicleIcon },
      { href: '/policies', label: 'Policies', icon: PolicyIcon },
      { href: '/kyc', label: 'KYC', icon: KycIcon },
    ],
  },
  {
    label: 'Service Network',
    items: [
      { href: '/garages', label: 'Garages', icon: GarageIcon },
      { href: '/feedback', label: 'Feedback', icon: FeedbackIcon },
    ],
  },
];

const STAFF_ROLES: readonly Role[] = ['AGENT', 'INSURER_ADMIN', 'ADMIN'];
const ADMIN_ROLES: readonly Role[] = ['INSURER_ADMIN', 'ADMIN'];

const userManagementItems = [
  { href: '/users', label: 'Users', roles: STAFF_ROLES },
  { href: '/roles', label: 'Roles', roles: STAFF_ROLES },
  { href: '/audit-trail', label: 'Audit Trail', roles: ADMIN_ROLES },
];

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

function VehicleIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M14 16H9m10 0h3v-3.15a1 1 0 0 0-.84-.99L16 10l-2.84-2.01A1 1 0 0 0 12.16 7H9c-1.38 0-2.37-.97-2.65-2.15a1 1 0 0 0-.84-1.19l-.93-.93-1.2.73a1 1 0 0 0-.44 1.22l.33.76c-.17.13-.37.26-.6.35l-.36.15C4.74 11.7 4 10.7 4 9.5 4 6.17 6.17 3.5 8.5 3.5h7c2.33 0 4.5 2.67 4.5 5.5 0 1.2-.4 2.2-.93 3.07l.73 1.2c.1.16.23.31.37.45l.01.01c.16.12.33.22.5.3l.76-.33a1 1 0 0 0 .44-.22l.73.54c.3-.16.55-.38.75-.63l.93.93c.22.22.47.4.76.53l.33-.17c1.16-.6 2.1-1.8 2.46-3.17a1 1 0 0 0-.46-1.17l-1.2-.73c.17-.13.32-.28.44-.45l.36-.15c.23-.1.44-.23.63-.38l-.55-.32.74-.55c-.08-.1-.17-.19-.27-.27z" />
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

export function AppLayout({ children }: AppLayoutProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [viewerRole, setViewerRole] = useState<Role | null>(null);

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((currentUser) => {
        if (!cancelled) setViewerRole(currentUser.role);
      })
      .catch(() => {
        if (!cancelled) setViewerRole(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const visibleManagementItems = viewerRole
    ? userManagementItems.filter((item) => item.roles.includes(viewerRole))
    : [];
  const showAdministration = visibleManagementItems.length > 0;
  const userManagementActive = visibleManagementItems.some(
    (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
  );
  const [userManagementOpen, setUserManagementOpen] = useState(userManagementActive);

  useEffect(() => {
    if (userManagementActive) setUserManagementOpen(true);
  }, [userManagementActive]);

  const handleLogout = (e: React.MouseEvent) => {
    e.preventDefault();
    logout().finally(() => {
      router.push('/');
    });
  };

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <Link href="/dashboard" className={styles.logo}>
            <svg viewBox="0 0 32 32" className={styles.logoIcon}>
              <rect x="2" y="2" width="28" height="28" rx="6" fill="#2563eb" />
              <path d="M10 22V10h4l6 6v6h-4l-6-6z" fill="white" />
            </svg>
            <span className={styles.logoText}>InsureClaim</span>
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

          {showAdministration && (
            <div className={styles.navGroup}>
              <p className={styles.groupLabel}>Administration</p>
              <button
                type="button"
                className={`${styles.navItem} ${styles.groupToggle} ${userManagementActive ? styles.navItemActive : ''}`}
                onClick={() => setUserManagementOpen((open) => !open)}
                aria-expanded={userManagementOpen}
                aria-controls="user-management-nav"
              >
                <AdminIcon />
                <span>User Management</span>
                <ChevronIcon expanded={userManagementOpen} />
              </button>
              <div
                className={styles.subNav}
                id="user-management-nav"
                hidden={!userManagementOpen}
              >
                {visibleManagementItems.map((item) => {
                  const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`${styles.subNavItem} ${isActive ? styles.subNavItemActive : ''}`}
                      aria-current={isActive ? 'page' : undefined}
                    >
                      <span className={styles.subNavMarker} aria-hidden="true" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          )}
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
          <div className={styles.headerUser}>
            <Avatar name="User" size="sm" />
            <span className={styles.userName}>User</span>
          </div>
        </header>
        <div className={styles.content}>{children}</div>
      </main>
    </div>
  );
}

function AdminIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5" aria-hidden="true">
      <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="10" cy="7" r="4" />
      <path d="M20 8v6m3-3h-6" />
    </svg>
  );
}

function ChevronIcon({ expanded }: { expanded: boolean }) {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      className={`${styles.chevron} ${expanded ? styles.chevronExpanded : ''}`}
      aria-hidden="true"
    >
      <path d="m5.5 7.5 4.5 4.5 4.5-4.5" />
    </svg>
  );
}
