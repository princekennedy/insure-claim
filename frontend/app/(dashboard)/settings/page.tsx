'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  Input,
  PageHeader,
  PageLoader,
  PasswordInput,
  Select,
} from '@/lib/components/ui';
import {
  getChatbotProviders,
  getSettings,
  testChatbotProvider,
  updateSettings,
} from '@/lib/services/settings';
import { getCurrentUser } from '@/lib/services/auth';
import type {
  ChatbotProvider,
  ChatbotTestResult,
  SettingGroup,
  SettingsResponse,
} from '@/lib/types';
import { KnowledgeBasePanel } from './knowledge-base-panel';
import styles from './settings.module.css';

const KNOWLEDGE_TAB = 'knowledge';

const GROUP_META: Record<string, { title: string; description: string }> = {
  chatbot: {
    title: 'AI Assistant',
    description:
      'How the 24/7 assistant answers customer questions and when it hands over to a human.',
  },
  claims: {
    title: 'Claims',
    description: 'Limits applied when customers file claims and receive settlement offers.',
  },
  fraud: {
    title: 'Fraud detection',
    description: 'Score at which a submitted claim is automatically flagged for review.',
  },
};

interface FieldMeta {
  label: string;
  hint?: string;
  min?: number;
  max?: number;
  step?: number;
}

const FIELD_META: Record<string, FieldMeta> = {
  enabled: { label: 'Assistant enabled', hint: 'When disabled, the assistant stays idle.' },
  provider: {
    label: 'AI provider',
    hint: 'The service the assistant sends messages to.',
  },
  model: { label: 'Model', hint: 'Model used for assistant replies.' },
  api_key: {
    label: 'API key',
    hint: 'Stored write-only and never shown again.',
  },
  max_tokens: { label: 'Max reply length', hint: 'Tokens per reply, 64 to 4096.' },
  escalate_threshold: {
    label: 'Escalation threshold',
    hint: 'Confidence from 0 to 1. Below it, a human picks up the conversation.',
    min: 0,
    max: 1,
    step: 0.05,
  },
  max_open_claims_per_customer: {
    label: 'Max open claims per customer',
    hint: 'Whole number between 1 and 100.',
    min: 1,
    max: 100,
  },
  settlement_window_days: {
    label: 'Settlement window (days)',
    hint: 'How long a settlement offer stays valid, 1 to 365.',
    min: 1,
    max: 365,
  },
  auto_alert_threshold: {
    label: 'Auto-flag threshold',
    hint: 'Fraud score from 0 to 100 that flags a claim automatically.',
    min: 0,
    max: 100,
  },
};

function toDraft(response: SettingsResponse): Record<string, string> {
  const draft: Record<string, string> = {};
  for (const group of response.groups) {
    for (const setting of group.settings) {
      draft[`${group.group}.${setting.key}`] = setting.value;
    }
  }
  return draft;
}

export default function SettingsPage() {
  const [response, setResponse] = useState<SettingsResponse | null>(null);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [savingGroup, setSavingGroup] = useState<string | null>(null);
  const [savedGroup, setSavedGroup] = useState<string | null>(null);
  const [groupErrors, setGroupErrors] = useState<Record<string, string>>({});
  const [activeTab, setActiveTab] = useState('');
  const [providers, setProviders] = useState<ChatbotProvider[]>([]);
  const [canReadKnowledge, setCanReadKnowledge] = useState(false);
  const [canManageKnowledge, setCanManageKnowledge] = useState(false);
  const [testingChatbot, setTestingChatbot] = useState(false);
  const [chatbotTest, setChatbotTest] = useState<ChatbotTestResult | null>(null);
  const [chatbotTestError, setChatbotTestError] = useState('');

  const loadSettings = useCallback(async () => {
    try {
      const data = await getSettings();
      setResponse(data);
      setDraft(toDraft(data));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load settings.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadSettings();
  }, [loadSettings]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const list = await getChatbotProviders();
        if (!cancelled) setProviders(list);
      } catch {
        // Non-fatal: the model field falls back to a free-text input.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const user = await getCurrentUser();
        if (cancelled) return;
        const permissions = user.permissions ?? [];
        setCanReadKnowledge(
          user.role === 'ADMIN' ||
            permissions.includes('KNOWLEDGE_READ') ||
            permissions.includes('KNOWLEDGE_MANAGE'),
        );
        setCanManageKnowledge(
          user.role === 'ADMIN' || permissions.includes('KNOWLEDGE_MANAGE'),
        );
      } catch {
        // The settings request already surfaces authentication/authorization failures.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  function setValue(draftKey: string, value: string) {
    setDraft((prev) => ({ ...prev, [draftKey]: value }));
    setSavedGroup(null);
    setChatbotTest(null);
    setChatbotTestError('');
  }

  function handleProviderChange(nextId: string) {
    setValue('chatbot.provider', nextId);
    const provider = providers.find((p) => p.id === nextId);
    const currentModel = draft['chatbot.model'] ?? '';
    if (provider && !provider.models.includes(currentModel)) {
      setValue('chatbot.model', provider.defaultModel);
    }
  }

  function validateGroup(group: SettingGroup): string {
    for (const setting of group.settings) {
      const value = (draft[`${group.group}.${setting.key}`] ?? setting.value).trim();
      if (setting.dataType === 'NUMBER' && !Number.isFinite(Number(value))) {
        return `${FIELD_META[setting.key]?.label ?? setting.key} must be a number.`;
      }
      if (setting.dataType === 'BOOLEAN' && value !== 'true' && value !== 'false') {
        return `${FIELD_META[setting.key]?.label ?? setting.key} must be enabled or disabled.`;
      }
    }
    return '';
  }

  async function handleSave(group: SettingGroup, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const invalid = validateGroup(group);
    if (invalid) {
      setGroupErrors((prev) => ({ ...prev, [group.group]: invalid }));
      return;
    }
    setSavingGroup(group.group);
    setGroupErrors((prev) => ({ ...prev, [group.group]: '' }));
    try {
      const updated = await updateSettings({
        groups: [
          {
            group: group.group,
            settings: group.settings.map((setting) => ({
              key: setting.key,
              value: (draft[`${group.group}.${setting.key}`] ?? setting.value).trim(),
            })),
          },
        ],
      });
      setResponse(updated);
      setDraft(toDraft(updated));
      setSavedGroup(group.group);
    } catch (err) {
      setGroupErrors((prev) => ({
        ...prev,
        [group.group]: err instanceof Error ? err.message : 'Settings could not be saved.',
      }));
    } finally {
      setSavingGroup(null);
    }
  }

  async function handleTestChatbot() {
    setTestingChatbot(true);
    setChatbotTest(null);
    setChatbotTestError('');
    try {
      const result = await testChatbotProvider({
        provider: draft['chatbot.provider'],
        model: draft['chatbot.model'],
        apiKey: draft['chatbot.api_key'],
      });
      setChatbotTest(result);
    } catch (err) {
      setChatbotTestError(
        err instanceof Error ? err.message : 'The connection test could not be completed.',
      );
    } finally {
      setTestingChatbot(false);
    }
  }

  const groups = response?.groups ?? [];
  const activeGroupData = groups.find((g) => g.group === activeTab) ?? groups[0];
  const currentTab = activeTab || activeGroupData?.group || '';
  const showingKnowledge = currentTab === KNOWLEDGE_TAB && canReadKnowledge;
  const activeMeta = activeGroupData
    ? GROUP_META[activeGroupData.group] ?? { title: activeGroupData.group, description: '' }
    : null;
  const activeError = activeGroupData ? groupErrors[activeGroupData.group] : '';
  const activeIsSaving = activeGroupData ? savingGroup === activeGroupData.group : false;
  const activeWasSaved = activeGroupData ? savedGroup === activeGroupData.group : false;

  return (
    <section className={styles.page}>
      <PageHeader
        title="Settings"
        description="Tune how the portal runs. Changes apply immediately, without a redeploy."
      />

      {error && (
        <>
          <Alert type="error">{error}</Alert>
          <div className={styles.errorActions}>
            <Button type="button" variant="secondary" onClick={() => void loadSettings()} loading={loading}>
              Try again
            </Button>
          </div>
        </>
      )}

      {loading && !response ? (
        <PageLoader message="Loading settings..." />
      ) : response ? (
        <>
          <div className={styles.tabs} role="tablist" aria-label="Settings sections">
            {groups.map((group) => {
              const isActive = group.group === currentTab;
              return (
                <button
                  key={group.group}
                  type="button"
                  role="tab"
                  id={`tab-${group.group}`}
                  aria-selected={isActive}
                  aria-controls={`panel-${group.group}`}
                  className={`${styles.tab} ${isActive ? styles.tabActive : ''}`}
                  onClick={() => setActiveTab(group.group)}
                >
                  {GROUP_META[group.group]?.title ?? group.group}
                </button>
              );
            })}
            {canReadKnowledge && (
              <button
                type="button"
                role="tab"
                id={`tab-${KNOWLEDGE_TAB}`}
                aria-selected={currentTab === KNOWLEDGE_TAB}
                aria-controls={`panel-${KNOWLEDGE_TAB}`}
                className={`${styles.tab} ${currentTab === KNOWLEDGE_TAB ? styles.tabActive : ''}`}
                onClick={() => setActiveTab(KNOWLEDGE_TAB)}
              >
                Knowledge base
              </button>
            )}
          </div>

          {showingKnowledge ? (
            <div
              className={styles.tabPanel}
              role="tabpanel"
              id={`panel-${KNOWLEDGE_TAB}`}
              aria-labelledby={`tab-${KNOWLEDGE_TAB}`}
            >
              <KnowledgeBasePanel canManage={canManageKnowledge} />
            </div>
          ) : activeGroupData && activeMeta && (
            <div
              key={activeGroupData.group}
              className={styles.tabPanel}
              role="tabpanel"
              id={`panel-${activeGroupData.group}`}
              aria-labelledby={`tab-${activeGroupData.group}`}
            >
              <Card>
                <CardHeader>
                  <CardTitle as="h2">{activeMeta.title}</CardTitle>
                  {activeMeta.description && <CardDescription>{activeMeta.description}</CardDescription>}
                </CardHeader>
                <form onSubmit={(event) => void handleSave(activeGroupData, event)}>
                  <CardContent>
                    <div className={styles.grid}>
                      {activeGroupData.settings.map((setting) => {
                        const draftKey = `${activeGroupData.group}.${setting.key}`;
                        const fieldMeta = FIELD_META[setting.key] ?? { label: setting.key };
                        const value = draft[draftKey] ?? setting.value;

                        if (setting.key === 'provider') {
                          const options = providers.length
                            ? providers.map((p) => ({ value: p.id, label: p.label }))
                            : (setting.options ?? []).map((id) => ({ value: id, label: id }));
                          return (
                            <Select
                              key={setting.key}
                              label={fieldMeta.label}
                              hint={fieldMeta.hint}
                              value={value}
                              options={options}
                              onChange={(event) => handleProviderChange(event.target.value)}
                            />
                          );
                        }

                        if (setting.key === 'model') {
                          const provider = providers.find(
                            (p) => p.id === (draft['chatbot.provider'] ?? value),
                          );
                          if (!provider) {
                            return (
                              <Input
                                key={setting.key}
                                label={fieldMeta.label}
                                hint={fieldMeta.hint}
                                value={value}
                                onChange={(event) => setValue(draftKey, event.target.value)}
                              />
                            );
                          }
                          const models =
                            value && !provider.models.includes(value)
                              ? [value, ...provider.models]
                              : provider.models;
                          return (
                            <Select
                              key={setting.key}
                              label={fieldMeta.label}
                              hint={fieldMeta.hint}
                              value={value}
                              options={models.map((m) => ({ value: m, label: m }))}
                              onChange={(event) => setValue(draftKey, event.target.value)}
                            />
                          );
                        }

                        if (setting.key === 'api_key') {
                          const provider = providers.find(
                            (p) => p.id === (draft['chatbot.provider'] ?? ''),
                          );
                          return (
                            <PasswordInput
                              key={setting.key}
                              label={fieldMeta.label}
                              showStrength={false}
                              autoComplete="off"
                              placeholder={
                                setting.configured
                                  ? 'A key is already stored'
                                  : provider?.keyPlaceholder ?? 'Paste your API key'
                              }
                              hint={
                                setting.configured
                                  ? 'A key is stored. Leave blank to keep it unchanged.'
                                  : provider?.keyHint ?? setting.description
                              }
                              value={value}
                              onChange={(event) => setValue(draftKey, event.target.value)}
                            />
                          );
                        }

                        if (setting.dataType === 'BOOLEAN') {
                          return (
                            <Select
                              key={setting.key}
                              label={fieldMeta.label}
                              hint={fieldMeta.hint}
                              value={value}
                              options={[
                                { value: 'true', label: 'Enabled' },
                                { value: 'false', label: 'Disabled' },
                              ]}
                              onChange={(event) => setValue(draftKey, event.target.value)}
                            />
                          );
                        }

                        return (
                          <Input
                            key={setting.key}
                            label={fieldMeta.label}
                            hint={fieldMeta.hint}
                            type={setting.dataType === 'NUMBER' ? 'number' : 'text'}
                            min={fieldMeta.min}
                            max={fieldMeta.max}
                            step={fieldMeta.step}
                            value={value}
                            onChange={(event) => setValue(draftKey, event.target.value)}
                          />
                        );
                      })}
                    </div>
                    {activeError && (
                      <Alert type="error" className={styles.groupError}>
                        {activeError}
                      </Alert>
                    )}
                    {activeWasSaved && !activeError && (
                      <Alert type="success" className={styles.groupError} dismissible onDismiss={() => setSavedGroup(null)}>
                        Saved. New values apply to the next request.
                      </Alert>
                    )}
                    {activeGroupData.group === 'chatbot' && chatbotTestError && (
                      <Alert type="error" className={styles.groupError}>
                        {chatbotTestError}
                      </Alert>
                    )}
                    {activeGroupData.group === 'chatbot' && chatbotTest && (
                      <Alert
                        type={chatbotTest.ok ? 'success' : 'error'}
                        className={styles.groupError}
                        dismissible
                        onDismiss={() => setChatbotTest(null)}
                      >
                        {chatbotTest.ok
                          ? `Connected to ${chatbotTest.provider} · ${chatbotTest.model} in ${chatbotTest.latencyMs} ms.${
                              chatbotTest.reply ? ` Reply: “${chatbotTest.reply}”` : ''
                            }`
                          : `Connection failed: ${chatbotTest.message}`}
                      </Alert>
                    )}
                  </CardContent>
                  <CardFooter className={styles.footer}>
                    <span className={styles.footerActions}>
                      {activeGroupData.group === 'chatbot' && (
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => void handleTestChatbot()}
                          loading={testingChatbot}
                        >
                          Test connection
                        </Button>
                      )}
                      <Button type="submit" loading={activeIsSaving}>
                        Save {activeMeta.title.toLowerCase()}
                      </Button>
                    </span>
                  </CardFooter>
                </form>
              </Card>
            </div>
          )}
        </>
      ) : null}
    </section>
  );
}
