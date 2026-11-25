import { redirect } from 'next/navigation';
import { requireStaffRole } from '@/lib/services/auth-server';
import { TaskQueue } from './task-queue';

export default async function TasksPage() {
  const user = await requireStaffRole();
  if (user.role !== 'OFFICER' && user.role !== 'MANAGER' && user.role !== 'ADMIN' && user.role !== 'INSURER_ADMIN') {
    redirect('/dashboard');
  }
  return <TaskQueue role={user.role} />;
}
