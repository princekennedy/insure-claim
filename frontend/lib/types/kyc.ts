export interface KycSubmissionRequest {
  documentType: KycDocumentType;
  documentNumber: string;
  fullNameOnDocument: string;
  dateOfBirth?: string;
  claimId?: number;
}

export interface KycRejectionRequest {
  reason: string;
  resubmitAllowed: boolean;
}

export interface KycVerificationResponse {
  id: number;
  status: KycStatus;
  statusLabel: string;
  documentType: KycDocumentType;
  documentLabel: string;
  maskedDocumentNumber: string;
  fullNameOnDocument: string;
  dateOfBirth: string | null;
  confidenceScore: number | null;
  failureReason: string | null;
  verifiedAt: string | null;
  verifiedBy: number | null;
  expiresAt: string | null;
  stillValid: boolean;
  submittedAt: string;
  updatedAt: string;
  claimId: number | null;
  resubmissionAllowed: boolean;
  reopenable: boolean;
  hasFrontImage: boolean;
  hasBackImage: boolean;
  hasSelfie: boolean;
}

export type KycDocumentType = 'NIC' | 'PASSPORT' | 'DRIVING_LICENSE';

export type KycImageSide = 'FRONT' | 'BACK' | 'SELFIE';

export type KycStatus = 'PENDING' | 'IN_REVIEW' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';

export const KYC_DOCUMENT_TYPE_LABELS: Record<KycDocumentType, string> = {
  NIC: 'National Identity Card',
  PASSPORT: 'Passport',
  DRIVING_LICENSE: 'Driving Licence',
};

export const KYC_STATUS_LABELS: Record<KycStatus, string> = {
  PENDING: 'Awaiting verification',
  IN_REVIEW: 'Under review',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
  EXPIRED: 'Expired',
};

export const KYC_DOCUMENT_TYPE_VALIDITY_YEARS: Record<KycDocumentType, number> = {
  NIC: 10,
  PASSPORT: 10,
  DRIVING_LICENSE: 10,
};
