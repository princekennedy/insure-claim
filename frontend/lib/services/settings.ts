import { request } from './api';
import { authenticatedRequest } from './auth';
import type { SettingsResponse, SettingsUpdateRequest } from '../types';

export function getSettings(): Promise<SettingsResponse> {
  return authenticatedRequest(() => request<SettingsResponse>('/admin/settings'));
}

export function updateSettings(body: SettingsUpdateRequest): Promise<SettingsResponse> {
  return authenticatedRequest(() =>
    request<SettingsResponse>('/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  );
}
