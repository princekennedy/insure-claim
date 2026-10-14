'use client';

import { ReactNode, useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Avatar } from '../ui/avatar';
import { Dialog } from '../ui/dialog';
import { Button } from '../ui/button';
import { getCurrentUser, logout } from '../../services/auth';
import type { Role } from '../../types/auth';
import styles from './app-layout.module.css';

interface AppLayoutProps {
  children: ReactNode;
}

interface NavItem {
  href: string;
  label: string;
  icon: React.ElementType;
  hasSubmenu?: boolean;
  subitems?: Array<{ href: string; label: string }>;
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

function CustomerIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
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

function ReportsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
      <line x1="18" y1="20" x2="18" y2="10" />
      <line x1="12" y1="20" x2="12" y2="4" />
      <line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}

function ChevronDownIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

const navGroups: Array<{ label: string; items: NavItem[] }> = [
  {
    label: 'Overview',
    items: [{ href: '/dashboard', label: 'Dashboard', icon: DashboardIcon }],
  },
  {
    label: 'Claims & Policies',
    items: [
      { href: '/claims', label: 'Claims', icon: ClaimsIcon },
      { href: '/customers', label: 'Customers', icon: CustomerIcon },
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
  {
    label: 'Reports',
    items: [
      { 
        href: '/reports', 
        label: 'Reports', 
        icon: ReportsIcon,
        hasSubmenu: true,
        subitems: [
          { href: '/reports/garage', label: 'Garage Performance' },
          { href: '/reports/claims', label: 'Claims' },
          { href: '/reports/financial', label: 'Financial' },
        ]
      },
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

const settingsItem = { href: '/settings', label: 'Settings', roles: ADMIN_ROLES };

export function AppLayout({ children }: AppLayoutProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [viewerRole, setViewerRole] = useState<Role | null>(null);
  const [currentUser, setCurrentUser] = useState<{ fullName: string } | null>(null);
  const [expandedSubmenu, setExpandedSubmenu] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((currentUser) => {
        if (!cancelled) {
          setViewerRole(currentUser.role);
          setCurrentUser({ fullName: currentUser.fullName });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setViewerRole(null);
          setCurrentUser(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const visibleManagementItems = viewerRole
    ? userManagementItems.filter((item) => item.roles.includes(viewerRole))
    : [];
  const showSettings = viewerRole !== null && settingsItem.roles.includes(viewerRole);
  const showAdministration = visibleManagementItems.length > 0 || showSettings;
  const settingsActive = pathname === settingsItem.href || pathname.startsWith(`${settingsItem.href}/`);
  const userManagementActive = visibleManagementItems.some(
    (item) => pathname === item.href || pathname.startsWith(`${item.href}/`),
  );
  const [userManagementOpenOverride, setUserManagementOpenOverride] = useState<boolean | null>(null);
  const userManagementOpen = userManagementOpenOverride ?? userManagementActive;
  const [logoutModalOpen, setLogoutModalOpen] = useState(false);

  const toggleSubmenu = (href: string) => {
    setExpandedSubmenu(expandedSubmenu === href ? null : href);
  };

  const handleLogout = () => {
    void logout().catch(() => undefined).then(() => router.replace('/'));
  };

  const handleLogoutClick = () => {
    setLogoutModalOpen(true);
  };

  const handleLogoutConfirm = () => {
    setLogoutModalOpen(false);
    handleLogout();
  };

  const handleLogoutCancel = () => {
    setLogoutModalOpen(false);
  };

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>
          <Link href="/dashboard" className={styles.logo}>
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
                const hasSubmenu = item.hasSubmenu;
                const subitems = item.subitems;
                const isExpanded = expandedSubmenu === item.href;

                if (hasSubmenu && subitems) {
                  return (
                    <div key={item.href}>
                      <button
                        type="button"
                        onClick={() => toggleSubmenu(item.href)}
                        className={`${styles.navItem} ${isActive ? styles.navItemActive : ''}`}
                        aria-expanded={isExpanded}
                      >
                        <Icon />
                        <span>{item.label}</span>
                        <ChevronDownIcon />
                      </button>
                      {isExpanded && (
                        <div className={styles.submenu}>
                          {subitems.map((subitem) => {
                            const isSubActive = pathname === subitem.href;
                            return (
                              <Link
                                key={subitem.href}
                                href={subitem.href}
                                className={`${styles.submenuItem} ${isSubActive ? styles.submenuItemActive : ''}`}
                              >
                                {subitem.label}
                              </Link>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                }

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
                onClick={() => setUserManagementOpenOverride(!userManagementOpen)}
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
              {showSettings && (
                <Link
                  href={settingsItem.href}
                  className={`${styles.navItem} ${settingsActive ? styles.navItemActive : ''}`}
                  aria-current={settingsActive ? 'page' : undefined}
                >
                  <SettingsIcon />
                  <span>{settingsItem.label}</span>
                </Link>
              )}
            </div>
          )}
        </nav>

        <div className={styles.sidebarFooter}>
          <button type="button" onClick={handleLogoutClick} className={styles.navItem}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span>Logout</span>
          </button>
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
            <Link href="/claims/new" className={styles.newClaimButton}>
              <PlusIcon />
              <span>New Claim</span>
            </Link>
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

      <Dialog
        isOpen={logoutModalOpen}
        onClose={handleLogoutCancel}
        title="Confirm Logout"
        size="sm"
        showCloseButton={false}
        closeOnOverlay={false}
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
            <Button variant="danger" onClick={handleLogoutConfirm}>
              Yes
            </Button>
            <Button variant="secondary" onClick={handleLogoutCancel}>
              No
            </Button>
          </div>
        }
      >
        <p>Are you sure you want to logout?</p>
      </Dialog>
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

function SettingsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5" aria-hidden="true">
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
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
