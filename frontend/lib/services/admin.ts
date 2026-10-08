import { request } from './api';
import { authenticatedRequest } from './auth';
import type {
  AuditLogResponse,
  PageResponse,
  RoleDefinition,
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

export function getAdminRoles(): Promise<RoleDefinition[]> {
  return authenticatedRequest(() => request<RoleDefinition[]>('/admin/roles'));
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
