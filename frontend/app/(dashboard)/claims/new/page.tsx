'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { PageHeader, Button } from '@/lib/components/ui';
import { ClaimSubmissionForm } from '@/lib/components/claims/claim-submission-form';
import styles from '../claims.module.css';

export default function NewClaimPage() {
  const router = useRouter();

  return (
    <div className={styles.page}>
      <PageHeader
        title="File a Claim"
        description="Provide details about the incident to start your claim"
        actions={
          <Link href="/claims" className={styles.newButton}>
            <Button variant="secondary">Back to Claims</Button>
          </Link>
        }
      />
      <ClaimSubmissionForm
        onSuccess={(claimId) => router.push(`/claims/${claimId}`)}
        onAddVehicle={() => router.push('/vehicles')}
      />
    </div>
  );
}