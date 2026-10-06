export interface FileClaimRequest {
  vehicleId: number;
  incidentType: IncidentType;
  incidentDate: string;
  incidentLocation?: string;
  description: string;
  estimatedAmount?: number;
  reportedByPolice: boolean;
  thirdPartyInvolved: boolean;
  requiresKyc: boolean;
}

export interface ClaimSummaryResponse {
  id: number;
  claimNumber: string;
  status: ClaimStatus;
  statusLabel: string;
  progressPercent: number;
  incidentType: IncidentType;
  incidentDate: string;
  vehicleRegistration: string;
  vehicleLabel: string;
  policyNumber: string;
  estimatedAmount: number | null;
  approvedAmount: number | null;
  netSettlement: number;
  excessAmount: number;
  isFraudFlagged: boolean;
  requiresKyc: boolean;
  submittedAt: string;
  settledAt: string | null;
  garageName: string | null;
  trackingToken: string | null;
}

export interface ClaimDetailResponse {
  summary: ClaimSummaryResponse;
  description: string;
  incidentLocation: string | null;
  incidentLocalDate: string;
  reportedByPolice: boolean;
  thirdPartyInvolved: boolean;
  fraudScore: number;
  customer: ClaimPartyResponse;
  vehicle: ClaimVehicleResponse;
  documents: ClaimDocumentResponse[];
  timeline: ClaimTimelineEntry[];
  nextStages: ClaimStageOption[];
  repair: RepairJobSummaryResponse | null;
  fraudAlerts: FraudAlertSummary[];
  kycStatus: string | null;
  canSubmitFeedback: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClaimPartyResponse {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
}

export interface ClaimVehicleResponse {
  id: number;
  registrationNumber: string;
  make: string;
  model: string;
  year: number;
  color: string | null;
}

export interface ClaimDocumentResponse {
  id: number;
  documentType: ClaimDocumentType;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  sizeLabel: string;
  uploadedAt: string;
  downloadUrl: string;
}

export interface ClaimTimelineEntry {
  fromStatus: ClaimStatus | null;
  toStatus: ClaimStatus;
  label: string;
  note: string | null;
  actorLabel: string;
  occurredAt: string;
  isCurrent: boolean;
}

export interface ClaimStageOption {
  status: ClaimStatus;
  label: string;
}

export interface RepairJobSummaryResponse {
  id: number;
  referenceCode: string;
  status: string;
  statusLabel: string;
  progressPercent: number;
  garageId: number;
  garageName: string;
  garagePhone: string;
  garageAddress: string;
  quotedAmount: number | null;
  finalAmount: number | null;
  estimatedDays: number | null;
  warrantyDays: number;
  assignedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  notes: string | null;
}

export interface FraudAlertSummary {
  id: number;
  ruleCode: string;
  category: string;
  severity: string;
  description: string;
  status: string;
  createdAt: string;
}

export interface UpdateClaimStatusRequest {
  status: ClaimStatus;
  note?: string;
  approvedAmount?: number;
  excessPaid?: number;
  rejectionReason?: string;
}

export interface ClaimTrackingResponse {
  claimNumber: string;
  status: ClaimStatus;
  statusLabel: string;
  progressPercent: number;
  incidentType: IncidentType;
  incidentDate: string;
  vehicleRegistration: string;
  timeline: ClaimTimelineEntry[];
  trackingTokenExpiresAt: string;
}

export interface IncidentTypeCount {
  incidentType: IncidentType;
  count: number;
}

export type ClaimStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'KYC_PENDING'
  | 'UNDER_REVIEW'
  | 'FRAUD_CHECK'
  | 'APPROVED'
  | 'REJECTED'
  | 'GARAGE_ASSIGNED'
  | 'IN_REPAIR'
  | 'READY_FOR_PICKUP'
  | 'SETTLED'
  | 'CLOSED'
  | 'WITHDRAWN';

export type IncidentType =
  | 'ACCIDENT'
  | 'THEFT'
  | 'FIRE'
  | 'FLOOD'
  | 'GLASS'
  | 'WINDSCREEN'
  | 'OTHER';

export type ClaimDocumentType =
  | 'DAMAGE_PHOTO'
  | 'POLICE_REPORT'
  | 'REPAIR_QUOTE'
  | 'INVOICE'
  | 'OWNERSHIP_DOCUMENT'
  | 'RECEIPT'
  | 'OTHER';

export const CLAIM_STATUS_LABELS: Record<ClaimStatus, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  KYC_PENDING: 'Awaiting KYC verification',
  UNDER_REVIEW: 'Under review',
  FRAUD_CHECK: 'Fraud screening',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  GARAGE_ASSIGNED: 'Garage assigned',
  IN_REPAIR: 'Repair in progress',
  READY_FOR_PICKUP: 'Ready for pickup',
  SETTLED: 'Settled',
  CLOSED: 'Closed',
  WITHDRAWN: 'Withdrawn',
};

export const INCIDENT_TYPE_LABELS: Record<IncidentType, string> = {
  ACCIDENT: 'Accident',
  THEFT: 'Theft',
  FIRE: 'Fire',
  FLOOD: 'Flood',
  GLASS: 'Glass Damage',
  WINDSCREEN: 'Windscreen',
  OTHER: 'Other',
};

export const CLAIM_DOCUMENT_TYPE_LABELS: Record<ClaimDocumentType, string> = {
  DAMAGE_PHOTO: 'Damage Photo',
  POLICE_REPORT: 'Police Report',
  REPAIR_QUOTE: 'Repair Quote',
  INVOICE: 'Invoice',
  OWNERSHIP_DOCUMENT: 'Ownership Document',
  RECEIPT: 'Receipt',
  OTHER: 'Other',
};

export const CLAIM_STATUS_PROGRESS: Record<ClaimStatus, number> = {
  DRAFT: 5,
  SUBMITTED: 15,
  KYC_PENDING: 25,
  UNDER_REVIEW: 40,
  FRAUD_CHECK: 50,
  APPROVED: 62,
  GARAGE_ASSIGNED: 72,
  IN_REPAIR: 82,
  READY_FOR_PICKUP: 92,
  SETTLED: 98,
  CLOSED: 100,
  REJECTED: 100,
  WITHDRAWN: 100,
};

export function formatCurrency(amount: number | null | undefined): string {
  if (amount === null || amount === undefined) return '-';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function formatDateTime(dateString: string): string {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
