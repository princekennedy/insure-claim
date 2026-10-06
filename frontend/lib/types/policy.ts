export interface CreatePolicyRequest {
  customerId?: number;
  vehicleId?: number;
  policyNumber?: string;
  startDate?: string;
  endDate?: string;
  premiumAmount?: number;
  sumInsured?: number;
  excessAmount?: number;
  insurerName?: string;
  productCode?: string;
  status?: PolicyStatus;
}

export interface UpdatePolicyRequest {
  endDate?: string;
  premiumAmount?: number;
  sumInsured?: number;
  excessAmount?: number;
  status?: PolicyStatus;
}

export interface PolicyResponse {
  id: number;
  policyNumber: string;
  insurerName: string;
  productCode: string;
  startDate: string;
  endDate: string;
  premiumAmount: number;
  sumInsured: number;
  excessAmount: number;
  status: string;
  vehicle: import('./vehicle').VehicleResponse;
  isCurrentlyValid: boolean;
}

export interface PolicyAdminResponse extends PolicyResponse {
  customer: import('./auth').UserResponse;
}

export interface PolicyPageResponse {
  content: PolicyResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type PolicyStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'LAPSED';

export const POLICY_STATUS_LABELS: Record<PolicyStatus, string> = {
  ACTIVE: 'Active',
  EXPIRED: 'Expired',
  CANCELLED: 'Cancelled',
  LAPSED: 'Lapsed',
};
