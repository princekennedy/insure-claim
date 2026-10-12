import { request, setTokens, clearTokens, refreshAccessToken, getRefreshToken, ApiError, isAuthenticated } from './api';
import type {
  RegisterRequest,
  LoginRequest,
  RefreshRequest,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  TokenResponse,
  UserResponse,
  UpdateProfileRequest,
  CustomerSummary,
} from '../types';

export async function register(data: RegisterRequest): Promise<TokenResponse> {
  const response = await request<TokenResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  setTokens(response.accessToken, response.refreshToken);
  return response;
}

export async function login(data: LoginRequest): Promise<TokenResponse> {
  const response = await request<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  setTokens(response.accessToken, response.refreshToken);
  return response;
}

export async function refresh(data: RefreshRequest): Promise<TokenResponse> {
  const response = await request<TokenResponse>('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify(data),
  });
  setTokens(response.accessToken, response.refreshToken);
  return response;
}

export async function logout(): Promise<void> {
  try {
    await request('/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: getRefreshToken() }),
    });
  } finally {
    clearTokens();
  }
}

export async function logoutAll(): Promise<void> {
  try {
    await request('/auth/logout-all', { method: 'POST' });
  } finally {
    clearTokens();
  }
}

export async function getCurrentUser(): Promise<UserResponse> {
  return request<UserResponse>('/auth/me');
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await request('/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  await request('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function resetPassword(data: ResetPasswordRequest): Promise<void> {
  await request('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getMeSummary(): Promise<CustomerSummary> {
  return request<CustomerSummary>('/me/summary');
}

export async function updateProfile(data: UpdateProfileRequest): Promise<UserResponse> {
  return request<UserResponse>('/me/profile', {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

// Re-export for convenience
export { isAuthenticated };

// Authenticated request wrapper. request() now refreshes on 401 internally
// (single-flight), so this only retries as a fallback for callers whose fn
// throws a raw 401 without going through the refresh path.
export async function authenticatedRequest<T>(
  fn: () => Promise<T>,
): Promise<T> {
  try {
    return await fn();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && canAttemptExternalRefresh()) {
      await refreshAccessToken();
      return await fn();
    }
    throw error;
  }
}

function canAttemptExternalRefresh(): boolean {
  return getRefreshToken() !== null;
}
