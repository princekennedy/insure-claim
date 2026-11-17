'use client';

import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Input,
  PageLoader,
  Select,
  Textarea,
} from '@/lib/components/ui';
import {
  createKnowledgeEntry,
  deleteKnowledgeEntry,
  getKnowledgeEntries,
  updateKnowledgeEntry,
} from '@/lib/services/knowledge';
import type { KnowledgeEntry, KnowledgeEntryRequest } from '@/lib/types';
import styles from './knowledge-base-panel.module.css';

interface KnowledgeBasePanelProps {
  canManage: boolean;
}

interface Draft {
  title: string;
  category: string;
  content: string;
  enabled: boolean;
}

const EMPTY_DRAFT: Draft = {
  title: '',
  category: 'General',
  content: '',
  enabled: true,
};

export function KnowledgeBasePanel({ canManage }: KnowledgeBasePanelProps) {
  const [entries, setEntries] = useState<KnowledgeEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [draft, setDraft] = useState<Draft | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function refresh() {
    try {
      const data = await getKnowledgeEntries();
      setEntries(data);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load the knowledge base.');
    }
  }

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const data = await getKnowledgeEntries();
        if (!cancelled) {
          setEntries(data);
          setError('');
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Unable to load the knowledge base.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  function startCreate() {
    setEditingId(null);
    setDraft({ ...EMPTY_DRAFT });
    setSuccess('');
    setError('');
  }

  function startEdit(entry: KnowledgeEntry) {
    setEditingId(entry.id);
    setDraft({
      title: entry.title,
      category: entry.category,
      content: entry.content,
      enabled: entry.enabled,
    });
    setSuccess('');
    setError('');
  }

  function cancelEdit() {
    setDraft(null);
    setEditingId(null);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft) return;
    const body: KnowledgeEntryRequest = {
      title: draft.title.trim(),
      category: draft.category.trim() || 'General',
      content: draft.content.trim(),
      enabled: draft.enabled,
    };
    if (!body.title || !body.content) {
      setError('Title and content are required.');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      if (editingId === null) {
        await createKnowledgeEntry(body);
        setSuccess('Article published. It applies to the next assistant reply.');
      } else {
        await updateKnowledgeEntry(editingId, body);
        setSuccess('Article updated. The assistant uses the new text immediately.');
      }
      setDraft(null);
      setEditingId(null);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'The article could not be saved.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(entry: KnowledgeEntry) {
    if (!window.confirm(`Delete "${entry.title}"? This cannot be undone.`)) return;
    setDeletingId(entry.id);
    setError('');
    setSuccess('');
    try {
      await deleteKnowledgeEntry(entry.id);
      setSuccess(`"${entry.title}" was deleted.`);
      if (editingId === entry.id) {
        setDraft(null);
        setEditingId(null);
      }
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'The article could not be deleted.');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle as="h2">Knowledge base</CardTitle>
        <CardDescription>
          Articles the assistant treats as authoritative Britam information. Add individual
          articles for products, cover, claims steps, contacts and FAQs; only enabled articles are
          sent to the model.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {error && (
          <Alert type="error" className={styles.notice} dismissible onDismiss={() => setError('')}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert
            type="success"
            className={styles.notice}
            dismissible
            onDismiss={() => setSuccess('')}
          >
            {success}
          </Alert>
        )}

        {canManage && !draft && (
          <div className={styles.toolbar}>
            <Button type="button" onClick={startCreate}>
              New article
            </Button>
          </div>
        )}

        {draft && (
          <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
            <div className={styles.formGrid}>
              <Input
                label="Title"
                maxLength={160}
                placeholder="e.g. Comprehensive motor cover"
                value={draft.title}
                onChange={(event) => setDraft({ ...draft, title: event.target.value })}
              />
              <Input
                label="Category"
                maxLength={64}
                placeholder="General"
                value={draft.category}
                onChange={(event) => setDraft({ ...draft, category: event.target.value })}
              />
              <Select
                label="Status"
                value={draft.enabled ? 'true' : 'false'}
                options={[
                  { value: 'true', label: 'Enabled' },
                  { value: 'false', label: 'Disabled' },
                ]}
                onChange={(event) => setDraft({ ...draft, enabled: event.target.value === 'true' })}
              />
            </div>
            <Textarea
              label="Content"
              hint="Plain text. Up to 20,000 characters per article."
              rows={8}
              maxLength={20000}
              placeholder="Explain the cover, exclusions, steps or contact details the assistant should know."
              value={draft.content}
              onChange={(event) => setDraft({ ...draft, content: event.target.value })}
            />
            <div className={styles.formActions}>
              <Button type="button" variant="secondary" onClick={cancelEdit} disabled={saving}>
                Cancel
              </Button>
              <Button type="submit" loading={saving}>
                {editingId === null ? 'Publish article' : 'Save changes'}
              </Button>
            </div>
          </form>
        )}

        {loading && entries.length === 0 ? (
          <PageLoader message="Loading knowledge base..." />
        ) : entries.length === 0 ? (
          <p className={styles.empty}>
            No articles yet. Add the first one so the assistant can answer Britam-specific
            questions.
          </p>
        ) : (
          <ul className={styles.list}>
            {entries.map((entry) => (
              <li key={entry.id} className={styles.entry}>
                <div className={styles.entryHeader}>
                  <div>
                    <h3 className={styles.entryTitle}>{entry.title}</h3>
                    <div className={styles.entryMeta}>
                      <span className={styles.badge}>{entry.category}</span>
                      <span className={entry.enabled ? styles.enabled : styles.disabled}>
                        {entry.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                      <span>Updated {new Date(entry.updatedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                  {canManage && (
                    <div className={styles.entryActions}>
                      <Button type="button" variant="secondary" onClick={() => startEdit(entry)}>
                        Edit
                      </Button>
                      <Button
                        type="button"
                        variant="secondary"
                        loading={deletingId === entry.id}
                        onClick={() => void handleDelete(entry)}
                      >
                        Delete
                      </Button>
                    </div>
                  )}
                </div>
                <p className={styles.entryContent}>{entry.content}</p>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
