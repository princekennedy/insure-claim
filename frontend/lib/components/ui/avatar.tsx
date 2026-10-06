'use client';

import styles from './avatar.module.css';

interface AvatarProps {
  src?: string | null;
  name: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter((part) => part.length > 0)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}

export function Avatar({ src, name, size = 'md', className = '' }: AvatarProps) {
  const sizeClass = styles[`size-${size}`];
  const initials = getInitials(name);

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={`${styles.avatar} ${sizeClass} ${className}`}
      />
    );
  }

  return (
    <div className={`${styles.avatar} ${styles.initials} ${sizeClass} ${className}`}>
      {initials}
    </div>
  );
}
