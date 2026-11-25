import { request } from './api';
import { authenticatedRequest } from './auth';
import type { ClaimTask, TaskOfficer } from '../types/task';

export function getTasks(): Promise<ClaimTask[]> {
  return authenticatedRequest(() => request<ClaimTask[]>('/tasks'));
}

export function getTaskOfficers(): Promise<TaskOfficer[]> {
  return authenticatedRequest(() => request<TaskOfficer[]>('/tasks/officers'));
}

export function claimTask(id: number): Promise<ClaimTask> {
  return authenticatedRequest(() => request<ClaimTask>(`/tasks/${id}/claim`, { method: 'POST' }));
}

export function assignTask(id: number, officerId: number): Promise<ClaimTask> {
  return authenticatedRequest(() => request<ClaimTask>(`/tasks/${id}/assignment`, {
    method: 'PUT',
    body: JSON.stringify({ officerId }),
  }));
}
