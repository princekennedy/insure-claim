'use client';

import { Button } from '@/lib/components/ui';
import styles from './report-export-actions.module.css';

export interface ReportExportSection {
  title: string;
  rows: Array<Record<string, string | number>>;
}

interface ReportExportActionsProps {
  title: string;
  sections: ReportExportSection[];
}

function escapeCsvValue(value: string | number): string {
  const text = String(value);
  const safeText = typeof value === 'string' && /^[\s\u0000-\u001f]*[=+\-@]/.test(text)
    ? `'${text}`
    : text;
  return `"${safeText.replace(/"/g, '""')}"`;
}

function createCsv(title: string, sections: ReportExportSection[]): string {
  const columns = [...new Set(sections.flatMap(({ rows }) => rows.flatMap(Object.keys)))];
  const records = [
    ['Report', 'Section', ...columns],
    ...sections.flatMap(({ title: sectionTitle, rows }) =>
      rows.map((row) => [
        title,
        sectionTitle,
        ...columns.map((column) => row[column] ?? ''),
      ]),
    ),
  ];

  return `\uFEFF${records
    .map((record) => record.map((value) => escapeCsvValue(value)).join(','))
    .join('\r\n')}`;
}

function createFilename(title: string, extension: string): string {
  const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  return `${slug || 'report'}.${extension}`;
}

export function ReportExportActions({ title, sections }: ReportExportActionsProps) {
  const exportCsv = () => {
    const file = new Blob([createCsv(title, sections)], {
      type: 'text/csv;charset=utf-8;',
    });
    const url = URL.createObjectURL(file);
    const link = document.createElement('a');
    link.href = url;
    link.download = createFilename(title, 'csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  const exportPdf = () => {
    const cleanUp = () => document.body.removeAttribute('data-report-print');
    window.addEventListener('afterprint', cleanUp, { once: true });
    document.body.setAttribute('data-report-print', 'true');
    window.print();
  };

  return (
    <div className={styles.actions} data-report-export-actions>
      <Button type="button" variant="secondary" onClick={exportCsv}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true" className={styles.icon}>
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <path d="M14 2v6h6M8 13h8M8 17h8" />
        </svg>
        Export CSV
      </Button>
      <Button type="button" variant="primary" onClick={exportPdf}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true" className={styles.icon}>
          <path d="M6 9V2h12v7M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
          <path d="M6 14h12v8H6z" />
        </svg>
        Export PDF
      </Button>
    </div>
  );
}
