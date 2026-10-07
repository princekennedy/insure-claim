export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  nic?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserResponse;
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  initials: string;
  lastLoginAt: string | null;
}

export interface UpdateProfileRequest {
  fullName?: string;
  phone?: string;
  nic?: string;
}

export type Role = 'CUSTOMER' | 'AGENT' | 'INSURER_ADMIN' | 'ADMIN';

export function isStaffRole(role: Role): boolean {
  return role !== 'CUSTOMER';
}
