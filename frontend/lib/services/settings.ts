import { request } from './api';
import { authenticatedRequest } from './auth';
import type {
  ChatbotProvider,
  ChatbotTestRequest,
  ChatbotTestResult,
  SettingsResponse,
  SettingsUpdateRequest,
} from '../types';

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

export function testChatbotProvider(body: ChatbotTestRequest): Promise<ChatbotTestResult> {
  return authenticatedRequest(() =>
    request<ChatbotTestResult>('/admin/settings/chatbot/test', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  );
}
