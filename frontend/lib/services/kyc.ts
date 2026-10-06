import { request, API_BASE_URL } from './api';
import type {
  KycSubmissionRequest,
  KycRejectionRequest,
  KycVerificationResponse,
  KycImageSide,
  PageResponse,
} from '../types';

export interface KycImageFiles {
  frontImage?: File;
  backImage?: File;
  selfie?: File;
}

export async function submitKyc(
  submission: KycSubmissionRequest,
  images: KycImageFiles = {},
): Promise<KycVerificationResponse> {
  // Backend expects a multipart request: a JSON part named "submission"
  // plus optional image parts "frontImage", "backImage", "selfie".
  const formData = new FormData();
  formData.append(
    'submission',
    new Blob([JSON.stringify(submission)], { type: 'application/json' }),
    'submission.json',
  );
  if (images.frontImage) formData.append('frontImage', images.frontImage);
  if (images.backImage) formData.append('backImage', images.backImage);
  if (images.selfie) formData.append('selfie', images.selfie);

  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const response = await fetch(`${API_BASE_URL}/api/v1/kyc/verifications`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: formData,
  });

  if (!response.ok) {
    let message = `KYC submission failed (HTTP ${response.status})`;
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

export async function getCurrentKyc(): Promise<KycVerificationResponse | null> {
  // May return 204 No Content when nothing has been submitted yet.
  return request<KycVerificationResponse | null>('/kyc/verifications/current');
}

export async function getKycVerifications(): Promise<KycVerificationResponse[]> {
  return request<KycVerificationResponse[]>('/kyc/verifications');
}

export async function getKycVerification(verificationId: number): Promise<KycVerificationResponse> {
  return request<KycVerificationResponse>(`/kyc/verifications/${verificationId}`);
}

export async function getKycImageUrl(verificationId: number, side: KycImageSide): Promise<string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const response = await fetch(
    `${API_BASE_URL}/api/v1/admin/kyc/verifications/${verificationId}/images/${side}`,
    { headers: token ? { Authorization: `Bearer ${token}` } : undefined },
  );
  if (!response.ok) {
    throw new Error(`Failed to load KYC image (HTTP ${response.status})`);
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}

export async function getAdminKycVerifications(
  page = 0,
  size = 20,
  status = 'PENDING',
): Promise<PageResponse<KycVerificationResponse>> {
  const out = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) out.set('status', status);
  return request<PageResponse<KycVerificationResponse>>(`/admin/kyc/verifications?${out.toString()}`);
}

export async function approveKyc(verificationId: number): Promise<KycVerificationResponse> {
  return request<KycVerificationResponse>(`/admin/kyc/verifications/${verificationId}/approve`, {
    method: 'POST',
  });
}

export async function rejectKyc(
  verificationId: number,
  data: KycRejectionRequest,
): Promise<KycVerificationResponse> {
  return request<KycVerificationResponse>(`/admin/kyc/verifications/${verificationId}/reject`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}