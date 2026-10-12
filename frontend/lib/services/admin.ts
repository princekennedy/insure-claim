import { request } from './api';
import { authenticatedRequest } from './auth';
import type {
  AuditLogResponse,
  CreateUserRequest,
  PageResponse,
  PermissionDefinition,
  RoleCreateRequest,
  RoleDefinition,
  RoleUpdateRequest,
  UpdateUserRequest,
  UserDetail,
  UserSummary,
} from '../types';

export interface UserListFilters {
  query?: string;
  role?: string;
}

export interface AuditListFilters {
  query?: string;
  action?: string;
  success?: boolean;
  from?: string;
  to?: string;
}

function toQuery(params: Record<string, string | number | boolean | undefined>): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') query.set(key, String(value));
  }
  const serialized = query.toString();
  return serialized ? `?${serialized}` : '';
}

export function getUsers(
  page = 0,
  size = 20,
  filters: UserListFilters = {},
): Promise<PageResponse<UserSummary>> {
  return authenticatedRequest(() =>
    request<PageResponse<UserSummary>>(
      `/users${toQuery({ query: filters.query, role: filters.role, page, size })}`,
    ),
  );
}

export function getAdminRoles(
  page = 0,
  size = 100,
): Promise<PageResponse<RoleDefinition>> {
  return authenticatedRequest(() =>
    request<PageResponse<RoleDefinition>>(`/admin/roles${toQuery({ page, size })}`),
  );
}

export function getPermissions(): Promise<PermissionDefinition[]> {
  return authenticatedRequest(() => request<PermissionDefinition[]>('/admin/roles/permissions'));
}

export function createRole(body: RoleCreateRequest): Promise<RoleDefinition> {
  return authenticatedRequest(() =>
    request<RoleDefinition>('/admin/roles', { method: 'POST', body: JSON.stringify(body) }),
  );
}

export function updateRole(code: string, body: RoleUpdateRequest): Promise<RoleDefinition> {
  return authenticatedRequest(() =>
    request<RoleDefinition>(`/admin/roles/${encodeURIComponent(code)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  );
}

export function deleteRole(code: string): Promise<void> {
  return authenticatedRequest(() =>
    request<void>(`/admin/roles/${encodeURIComponent(code)}`, { method: 'DELETE' }),
  );
}

export function getUser(userId: number): Promise<UserDetail> {
  return authenticatedRequest(() => request<UserDetail>(`/users/${userId}`));
}

export function createUser(body: CreateUserRequest): Promise<UserDetail> {
  return authenticatedRequest(() =>
    request<UserDetail>('/users', { method: 'POST', body: JSON.stringify(body) }),
  );
}

export function updateUser(userId: number, body: UpdateUserRequest): Promise<UserDetail> {
  return authenticatedRequest(() =>
    request<UserDetail>(`/users/${userId}`, { method: 'PUT', body: JSON.stringify(body) }),
  );
}

export function deleteUser(userId: number): Promise<void> {
  return authenticatedRequest(() =>
    request<void>(`/users/${userId}`, { method: 'DELETE' }),
  );
}

export function getAuditTrail(
  page = 0,
  size = 20,
  filters: AuditListFilters = {},
): Promise<PageResponse<AuditLogResponse>> {
  return authenticatedRequest(() =>
    request<PageResponse<AuditLogResponse>>(
      `/admin/audit${toQuery({ ...filters, page, size })}`,
    ),
  );
}

export function setUserEnabled(userId: number, enabled: boolean): Promise<void> {
  return authenticatedRequest(() =>
    request<void>(`/users/${userId}/enabled?enabled=${enabled}`, { method: 'PUT' }),
  );
}

export function setUserRole(userId: number, role: string): Promise<void> {
  return authenticatedRequest(() =>
    request<void>(`/users/${userId}/role?role=${encodeURIComponent(role)}`, { method: 'PUT' }),
  );
}
