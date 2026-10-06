import { request } from './api';
import type {
  GarageResponse,
  RepairJobResponse,
  AssignGarageRequest,
  UpdateRepairJobRequest,
  GarageFeedbackRequest,
  GarageFeedbackResponse,
  PageResponse,
} from '../types';

export async function getGarages(
  page = 0,
  size = 20,
  city?: string,
  query?: string,
): Promise<PageResponse<GarageResponse>> {
  const out = new URLSearchParams({ page: String(page), size: String(size) });
  if (city) out.set('city', city);
  if (query) out.set('query', query);
  return request<PageResponse<GarageResponse>>(`/garages?${out.toString()}`);
}

export async function getGarage(garageId: number): Promise<GarageResponse> {
  return request<GarageResponse>(`/garages/${garageId}`);
}

export async function getGarageFeedback(
  garageId: number,
  page = 0,
  size = 20,
): Promise<PageResponse<GarageFeedbackResponse>> {
  return request<PageResponse<GarageFeedbackResponse>>(
    `/garages/${garageId}/feedback?page=${page}&size=${size}`,
  );
}

export async function getRepairJobs(
  page = 0,
  size = 20,
  garageId?: number,
  status?: string,
): Promise<PageResponse<RepairJobResponse>> {
  const out = new URLSearchParams({ page: String(page), size: String(size) });
  if (garageId) out.set('garageId', String(garageId));
  if (status) out.set('status', status);
  return request<PageResponse<RepairJobResponse>>(`/repair-jobs?${out.toString()}`);
}

export async function getRepairJob(jobId: number): Promise<RepairJobResponse> {
  return request<RepairJobResponse>(`/repair-jobs/${jobId}`);
}

export async function assignGarage(
  claimId: number,
  data: AssignGarageRequest,
): Promise<RepairJobResponse> {
  return request<RepairJobResponse>(`/claims/${claimId}/garage`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function updateRepairJob(
  jobId: number,
  data: UpdateRepairJobRequest,
): Promise<RepairJobResponse> {
  return request<RepairJobResponse>(`/repair-jobs/${jobId}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export async function cancelRepairJob(jobId: number, reason?: string): Promise<RepairJobResponse> {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
  return request<RepairJobResponse>(`/repair-jobs/${jobId}${query}`, {
    method: 'DELETE',
  });
}

export async function submitFeedback(
  claimId: number,
  data: GarageFeedbackRequest,
): Promise<GarageFeedbackResponse> {
  return request<GarageFeedbackResponse>(`/claims/${claimId}/feedback`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getMyFeedback(
  page = 0,
  size = 20,
): Promise<PageResponse<GarageFeedbackResponse>> {
  return request<PageResponse<GarageFeedbackResponse>>(`/me/feedback?page=${page}&size=${size}`);
}