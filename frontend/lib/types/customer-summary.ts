import type { PolicyResponse } from './policy';

export interface CustomerSummary {
  totalClaims: number;
  openClaims: number;
  settledClaims: number;
  rejectedClaims: number;
  totalClaimedAmount: number;
  totalSettledAmount: number;
  activePolicy: PolicyResponse | null;
  kycVerified: boolean;
  unreadConcerns: number;
}