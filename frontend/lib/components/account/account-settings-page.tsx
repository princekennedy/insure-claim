'use client';

import { FormEvent, useEffect, useState } from 'react';
import { Alert, Avatar, Button, Input, PageLoader } from '@/lib/components/ui';
import { useTheme } from '@/lib/components/theme-provider';
import { getCurrentUser, updateProfile } from '@/lib/services/auth';
import type { UpdateProfileRequest, UserResponse } from '@/lib/types';
import styles from './account-settings-page.module.css';

interface ProfileForm {
  fullName: string;
  phone: string;
  nic: string;
}

const EMPTY_PROFILE: ProfileForm = { fullName: '', phone: '', nic: '' };

function toProfileForm(user: UserResponse): ProfileForm {
  return {
    fullName: user.fullName,
    phone: user.phone ?? '',
    nic: user.nic ?? '',
  };
}

export function AccountSettingsPage() {
  const { mode, error: themeError, setMode } = useTheme();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [originalProfile, setOriginalProfile] = useState<ProfileForm>(EMPTY_PROFILE);
  const [profileForm, setProfileForm] = useState<ProfileForm>(EMPTY_PROFILE);
  const [loading, setLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((currentUser) => {
        if (cancelled) return;
        const form = toProfileForm(currentUser);
        setUser(currentUser);
        setProfileForm(form);
        setOriginalProfile(form);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setProfileError(
            error instanceof Error ? error.message : 'We could not load your account details.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function updateProfileField(field: keyof ProfileForm, value: string) {
    setProfileForm((current) => ({ ...current, [field]: value }));
    setProfileError('');
    setProfileSuccess('');
  }

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setProfileError('');
    setProfileSuccess('');

    const fullName = profileForm.fullName.trim();
    const phone = profileForm.phone.trim();
    const nic = profileForm.nic.trim();
    if (fullName.length < 2) {
      setProfileError('Enter your full name using at least 2 characters.');
      return;
    }
    if (originalProfile.phone && !phone) {
      setProfileError('Enter a valid phone number. An existing number cannot be removed here.');
      return;
    }
    if (originalProfile.nic && !nic) {
      setProfileError('Enter your identification number. An existing number cannot be removed here.');
      return;
    }

    const changes: UpdateProfileRequest = { fullName };
    if (phone && phone !== originalProfile.phone) changes.phone = phone;
    if (nic && nic !== originalProfile.nic) changes.nic = nic;

    setProfileSaving(true);
    try {
      const updatedUser = await updateProfile(changes);
      const updatedForm = toProfileForm(updatedUser);
      setUser(updatedUser);
      setProfileForm(updatedForm);
      setOriginalProfile(updatedForm);
      setProfileSuccess('Your profile details have been updated.');
    } catch (error) {
      setProfileError(
        error instanceof Error ? error.message : 'Your profile could not be saved. Please try again.',
      );
    } finally {
      setProfileSaving(false);
    }
  }

  if (loading) return <PageLoader message="Loading your account..." />;

  const roleLabel = user?.role.replaceAll('_', ' ').toLowerCase() ?? 'account holder';
  const isProfileDirty =
    profileForm.fullName.trim() !== originalProfile.fullName ||
    profileForm.phone.trim() !== originalProfile.phone ||
    profileForm.nic.trim() !== originalProfile.nic;

  return (
    <section className={styles.page}>
      <header className={styles.pageHeading}>
        <div>
          <p className={styles.eyebrow}>YOUR BRITAM ACCOUNT</p>
          <h1>Account settings</h1>
          <p className={styles.intro}>
            Keep your details current so we can support you when it matters.
          </p>
        </div>
        <div className={styles.accountStamp}>
          <span className={styles.stampLabel}>SIGNED IN AS</span>
          <span className={styles.stampValue}>{roleLabel}</span>
        </div>
      </header>

      {profileError && (
        <Alert type="error" dismissible onDismiss={() => setProfileError('')}>
          {profileError}
        </Alert>
      )}
      {profileSuccess && (
        <Alert type="success" dismissible onDismiss={() => setProfileSuccess('')}>
          {profileSuccess}
        </Alert>
      )}

      {!user ? (
        <section className={styles.loadError} aria-live="polite">
          <span className={styles.errorMark} aria-hidden="true">!</span>
          <div>
            <h2>Account details are unavailable</h2>
            <p>Refresh the page to try loading your profile again.</p>
          </div>
        </section>
      ) : (
        <div className={styles.settingsLayout}>
          <aside className={styles.profileAside}>
            <div className={styles.identityMark}>
              <Avatar name={user.fullName} size="lg" />
            </div>
            <h2>{user.fullName}</h2>
            <p className={styles.accountEmail}>{user.email}</p>
            <div className={styles.accountStatus}>
              <span className={styles.statusDot} />
              <span>{user.enabled ? 'Account active' : 'Account disabled'}</span>
            </div>
            <div className={styles.asideRule} />
            <p className={styles.asideNote}>
              Your profile information is used to verify your identity and keep your policies up to date.
            </p>
          </aside>

          <div className={styles.settingsSections}>
            <section className={styles.section} aria-labelledby="profile-title">
              <div className={styles.sectionHeading}>
                <span className={styles.sectionNumber}>01</span>
                <div>
                  <h2 id="profile-title">Personal information</h2>
                  <p>Update the details associated with your account.</p>
                </div>
              </div>

              <form className={styles.form} onSubmit={handleProfileSubmit}>
                <div className={styles.formGrid}>
                  <Input
                    label="Full name"
                    name="fullName"
                    autoComplete="name"
                    value={profileForm.fullName}
                    onChange={(event) => updateProfileField('fullName', event.target.value)}
                    required
                    minLength={2}
                    maxLength={160}
                  />
                  <Input
                    label="Email address"
                    name="email"
                    type="email"
                    value={user.email}
                    disabled
                    hint="Email changes are managed by your support team."
                  />
                  <Input
                    label="Phone number"
                    name="phone"
                    type="tel"
                    autoComplete="tel"
                    value={profileForm.phone}
                    onChange={(event) => updateProfileField('phone', event.target.value)}
                    pattern="\+?[0-9 ()-]{7,32}"
                    maxLength={32}
                    hint="Use the number where we can reach you about your cover."
                  />
                  <Input
                    label="National identification number"
                    name="nic"
                    autoComplete="off"
                    value={profileForm.nic}
                    onChange={(event) => updateProfileField('nic', event.target.value)}
                    maxLength={32}
                    hint="Used to support identity verification."
                  />
                </div>
                <div className={styles.formFooter}>
                  <span className={styles.formFootnote}>Changes are saved to your secure account.</span>
                  <Button type="submit" loading={profileSaving} disabled={!isProfileDirty}>
                    Save changes
                  </Button>
                </div>
              </form>
            </section>

            <section className={styles.section} aria-labelledby="appearance-title">
              <div className={styles.sectionHeading}>
                <span className={styles.sectionNumber}>02</span>
                <div>
                  <h2 id="appearance-title">Appearance</h2>
                  <p>Choose light or dark mode for the portal.</p>
                </div>
              </div>

              {themeError && (
                <Alert type="error">
                  {themeError}
                </Alert>
              )}
              <div className={styles.themeOptions} role="group" aria-label="Color theme">
                {(['light', 'dark'] as const).map((theme) => (
                  <button
                    key={theme}
                    type="button"
                    className={`${styles.themeOption} ${mode === theme ? styles.themeOptionActive : ''}`}
                    aria-pressed={mode === theme}
                    onClick={() => setMode(theme)}
                  >
                    {theme === 'light' ? 'Light mode' : 'Dark mode'}
                  </button>
                ))}
              </div>
            </section>

          </div>
        </div>
      )}
    </section>
  );
}
