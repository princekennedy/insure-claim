'use client';

import { useState } from 'react';
import { EmptyState, PageHeader } from '@/lib/components/ui';
import type { Role } from '@/lib/types/auth';
import { KycReviewQueue } from './kyc-review-queue';
import { TaskQueue } from './task-queue';

export type QueueStatus = 'loading' | 'error' | 'empty' | 'has-tasks';

export function showEmptyQueue(kycStatus: QueueStatus, claimStatus: QueueStatus, canViewClaimTasks: boolean): boolean {
  return kycStatus === 'empty' && (!canViewClaimTasks || claimStatus === 'empty');
}

export function TasksOverview({ role }: { role: Role }) {
  const [kycStatus, setKycStatus] = useState<QueueStatus>('loading');
  const [claimStatus, setClaimStatus] = useState<QueueStatus>('loading');
  const canViewClaimTasks = role === 'OFFICER' || role === 'MANAGER' || role === 'ADMIN' || role === 'INSURER_ADMIN';
  const allEmpty = showEmptyQueue(kycStatus, claimStatus, canViewClaimTasks);

  return (
    <div>
      <PageHeader title="Tasks" description="Review outstanding work and keep it moving." />
      <KycReviewQueue onStatusChange={setKycStatus} />
      {canViewClaimTasks && <TaskQueue role={role} onStatusChange={setClaimStatus} />}
      {allEmpty && <EmptyState title="No tasks in the queue" description="Review tasks will appear here when available." />}
    </div>
  );
}
