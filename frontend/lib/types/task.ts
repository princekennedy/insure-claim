export interface ClaimTask {
  id: number;
  claimId: number;
  claimNumber: string;
  customerName: string;
  vehicleRegistration: string;
  claimStatus: string;
  submittedAt: string;
  assigneeId?: number | null;
  assigneeName?: string | null;
  assignedAt?: string | null;
  assignedByName?: string | null;
}

export interface TaskOfficer {
  id: number;
  fullName: string;
}
