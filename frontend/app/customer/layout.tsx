import { CustomerLayout as Layout } from "@/lib/components/layout";
import { requireCustomerRole } from "@/lib/services/auth-server";

export default async function CustomerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Server-side authentication and role guard
  await requireCustomerRole();

  return <Layout>{children}</Layout>;
}
