import { requireStaffRole } from '@/lib/services/auth-server';
import { TasksOverview } from './tasks-overview';

export default async function TasksPage() {
  const user = await requireStaffRole();
  return <TasksOverview role={user.role} />;
}
