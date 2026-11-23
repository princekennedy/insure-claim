export type SettingDataType = 'STRING' | 'NUMBER' | 'BOOLEAN';

export interface SettingValue {
  key: string;
  value: string;
  dataType: SettingDataType;
  description: string;
  secret?: boolean;
  configured?: boolean;
  options?: string[];
}

export interface ChatbotProvider {
  id: string;
  label: string;
  defaultModel: string;
  models: string[];
  keyPlaceholder: string;
  keyHint: string;
  docsUrl: string;
}

export interface SettingGroup {
  group: string;
  settings: SettingValue[];
}

export interface SettingsResponse {
  groups: SettingGroup[];
}

export interface SettingUpdate {
  key: string;
  value: string;
}

export interface SettingGroupUpdate {
  group: string;
  settings: SettingUpdate[];
}

export interface SettingsUpdateRequest {
  groups: SettingGroupUpdate[];
}

export interface ChatbotTestRequest {
  provider?: string;
  model?: string;
  apiKey?: string;
}

export interface KnowledgeEntry {
  id: number;
  title: string;
  category: string;
  content: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeEntryRequest {
  title: string;
  category: string;
  content: string;
  enabled: boolean;
}

export interface ChatbotTestResult {
  ok: boolean;
  provider: string;
  model: string;
  latencyMs: number;
  message: string;
  reply?: string | null;
}
