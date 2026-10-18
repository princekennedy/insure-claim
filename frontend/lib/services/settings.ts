import { request } from './api';
import { authenticatedRequest } from './auth';
import type { ChatbotProvider, SettingsResponse, SettingsUpdateRequest } from '../types';

export function getSettings(): Promise<SettingsResponse> {
  return authenticatedRequest(() => request<SettingsResponse>('/admin/settings'));
}

export function getChatbotProviders(): Promise<ChatbotProvider[]> {
  return authenticatedRequest(() => request<ChatbotProvider[]>('/admin/settings/chatbot/providers'));
}

export function updateSettings(body: SettingsUpdateRequest): Promise<SettingsResponse> {
  return authenticatedRequest(() =>
    request<SettingsResponse>('/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  );
}
