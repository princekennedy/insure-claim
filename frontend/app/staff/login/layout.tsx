import { AuthLayout } from '@/lib/components/layout';

export default function StaffLoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AuthLayout>{children}</AuthLayout>;
}
