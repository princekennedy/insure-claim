import { request, API_BASE_URL } from './api';
import { authenticatedRequest } from './auth';
import type {
  FileClaimRequest,
  ClaimSummaryResponse,
  ClaimDetailResponse,
  ClaimDocumentResponse,
  UpdateClaimStatusRequest,
  PageResponse,
} from '../types';

export interface ClaimListFilters {
  status?: string | string[];
  incidentType?: string | string[];
  fraudOnly?: boolean;
  query?: string;
  from?: string;
  to?: string;
}

function toQuery(params: Record<string, unknown>): string {
  const out = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value)) {
      for (const entry of value) out.append(key, String(entry));
    } else {
      out.append(key, String(value));
    }
  }
  const text = out.toString();
  return text ? `?${text}` : '';
}

export async function fileClaim(data: FileClaimRequest): Promise<ClaimDetailResponse> {
  return request<ClaimDetailResponse>('/claims', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getClaims(
  page = 0,
  size = 20,
  filters: ClaimListFilters = {},
): Promise<PageResponse<ClaimSummaryResponse>> {
  return authenticatedRequest(() =>
    request<PageResponse<ClaimSummaryResponse>>(`/claims${toQuery({ page, size, ...filters })}`),
  );
}

export async function getMyClaims(
  page = 0,
  size = 20,
  filters: ClaimListFilters = {},
): Promise<PageResponse<ClaimSummaryResponse>> {
  return getClaims(page, size, filters);
}

export async function getClaim(claimId: number): Promise<ClaimDetailResponse> {
  return request<ClaimDetailResponse>(`/claims/${claimId}`);
}

export async function getClaimByNumber(claimNumber: string): Promise<ClaimDetailResponse> {
  return request<ClaimDetailResponse>(`/claims/by-number/${encodeURIComponent(claimNumber)}`);
}

export async function updateClaimStatus(
  claimId: number,
  data: UpdateClaimStatusRequest,
): Promise<ClaimDetailResponse> {
  return authenticatedRequest(() =>
    request<ClaimDetailResponse>(`/claims/${claimId}/status`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    }),
  );
}

export async function uploadDocument(
  claimId: number,
  file: File,
  documentType: string,
): Promise<ClaimDocumentResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('documentType', documentType);

  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const response = await fetch(`${API_BASE_URL}/api/v1/claims/${claimId}/documents`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: formData,
  });

  if (!response.ok) {
    let message = `Upload failed (HTTP ${response.status})`;
    try {
      const body = await response.json();
      message = body.message || message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }

  return response.json();
}

export async function deleteDocument(claimId: number, documentId: number): Promise<void> {
  await request(`/claims/${claimId}/documents/${documentId}`, {
    method: 'DELETE',
  });
}

export async function createTrackingLink(
  claimId: number,
): Promise<{ claimNumber: string; token: string; path: string }> {
  return request<{ claimNumber: string; token: string; path: string }>(
    `/claims/${claimId}/tracking-link`,
    { method: 'POST' },
  );
}