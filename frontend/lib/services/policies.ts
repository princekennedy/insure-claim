import { request } from './api';
import type {
  PolicyResponse,
  PolicyAdminResponse,
  CreatePolicyRequest,
  UpdatePolicyRequest,
  PageResponse,
} from '../types';

export async function getPolicies(): Promise<PolicyResponse[]> {
  return request<PolicyResponse[]>('/me/policies');
}

export async function getPolicy(policyId: number): Promise<PolicyResponse> {
  return request<PolicyResponse>(`/me/policies/${policyId}`);
}

export interface AdminPolicyFilters {
  customerId?: number;
  status?: string;
  query?: string;
}

export async function getAdminPolicies(
  page = 0,
  size = 20,
  filters: AdminPolicyFilters = {},
): Promise<PageResponse<PolicyAdminResponse>> {
  const out = new URLSearchParams({ page: String(page), size: String(size) });
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') out.set(key, String(value));
  }
  return request<PageResponse<PolicyAdminResponse>>(`/policies?${out.toString()}`);
}

export async function createPolicy(data: CreatePolicyRequest): Promise<PolicyAdminResponse> {
  return request<PolicyAdminResponse>('/policies', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updatePolicy(
  policyId: number,
  data: UpdatePolicyRequest,
): Promise<PolicyAdminResponse> {
  return request<PolicyAdminResponse>(`/policies/${policyId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}