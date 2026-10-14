import { AppLayout } from "@/lib/components/layout";
import { requireStaffRole } from "@/lib/services/auth-server";

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Server-side authentication and role guard
  await requireStaffRole();

  return <AppLayout>{children}</AppLayout>;
}
