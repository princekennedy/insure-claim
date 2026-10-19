import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

type Role = 'CUSTOMER' | 'AGENT' | 'INSURER_ADMIN' | 'ADMIN';

interface ServerUser {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  initials: string;
  lastLoginAt: string | null;
}

async function validateToken(accessToken: string): Promise<{ valid: boolean; user?: ServerUser }> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (response.ok) {
      const user = await response.json();
      return { valid: true, user };
    }
    return { valid: false };
  } catch {
    return { valid: false };
  }
}

export function isStaffRole(role: Role): boolean {
  return role !== 'CUSTOMER';
}

export async function requireAuth(): Promise<ServerUser> {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get('accessToken')?.value;

  if (!accessToken) {
    redirect('/login');
  }

  const validation = await validateToken(accessToken);

  if (!validation.valid || !validation.user) {
    // `blocked=1` marks this as a rejection (not a plain visit to /login):
    // the login page then drops the leftover tokens instead of redirecting
    // back here, which is what used to cause the /login <-> /dashboard loop.
    redirect('/login?blocked=1');
  }

  return validation.user;
}

export async function requireStaffRole(): Promise<ServerUser> {
  const user = await requireAuth();

  if (isStaffRole(user.role)) {
    return user;
  }

  redirect('/customer/home');
}

export async function requireCustomerRole(): Promise<ServerUser> {
  const user = await requireAuth();

  if (!isStaffRole(user.role)) {
    return user;
  }

  redirect('/dashboard');
}
