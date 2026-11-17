import { request } from './api';
import { authenticatedRequest } from './auth';
import type { KnowledgeEntry, KnowledgeEntryRequest } from '../types';

export function getKnowledgeEntries(): Promise<KnowledgeEntry[]> {
  return authenticatedRequest(() => request<KnowledgeEntry[]>('/admin/knowledge'));
}

export function createKnowledgeEntry(body: KnowledgeEntryRequest): Promise<KnowledgeEntry> {
  return authenticatedRequest(() =>
    request<KnowledgeEntry>('/admin/knowledge', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  );
}

export function updateKnowledgeEntry(
  id: number,
  body: KnowledgeEntryRequest,
): Promise<KnowledgeEntry> {
  return authenticatedRequest(() =>
    request<KnowledgeEntry>(`/admin/knowledge/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  );
}

export function deleteKnowledgeEntry(id: number): Promise<void> {
  return authenticatedRequest(() =>
    request<void>(`/admin/knowledge/${id}`, { method: 'DELETE' }),
  );
}
