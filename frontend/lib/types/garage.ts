export interface GarageResponse {
  id: number;
  code: string;
  name: string;
  address: string;
  city: string;
  contactPhone: string;
  contactEmail: string | null;
  panelRating: number | null;
  ratingAverage: number;
  ratingCount: number;
  complaintCount: number;
  jobsCompleted: number;
  avgTurnaroundDays: number | null;
  isPanelGarage: boolean;
  active: boolean;
  performanceStatus: GaragePerformanceStatus;
  performanceLabel: string;
  acceptingWork: boolean;
}

export interface RepairJobResponse {
  id: number;
  referenceCode: string;
  claimId: number;
  claimNumber: string;
  status: RepairJobStatus;
  statusLabel: string;
  progressPercent: number;
  garage: GarageResponse;
  quotedAmount: number | null;
  approvedAmount: number | null;
  finalAmount: number | null;
  assignedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  estimatedDays: number | null;
  warrantyDays: number;
  notes: string | null;
  turnaroundDays: number | null;
  canSubmitFeedback: boolean;
}

export interface AssignGarageRequest {
  garageId: number;
  quotedAmount?: number;
  approvedAmount?: number;
  estimatedDays?: number;
  notes?: string;
  warrantyDays: number;
}

export interface UpdateRepairJobRequest {
  status: RepairJobStatus;
  finalAmount?: number;
  notes?: string;
}

export interface GarageFeedbackRequest {
  overallRating: number;
  qualityRating?: number;
  timelinessRating?: number;
  priceFairnessRating?: number;
  staffCourtesyRating?: number;
  comments?: string;
  recommendAgain?: boolean;
}

export interface GarageFeedbackResponse {
  id: number;
  claimId: number;
  claimNumber: string;
  garageId: number;
  garageName: string;
  comments: string | null;
  overallRating: number;
  qualityRating: number | null;
  timelinessRating: number | null;
  priceFairnessRating: number | null;
  staffCourtesyRating: number | null;
  recommendAgain: boolean | null;
  isComplaint: boolean;
  createdAt: string;
  commentVisible: boolean;
}

export interface GaragePerformanceSummary {
  garageId: number;
  garageName: string;
  performanceStatus: GaragePerformanceStatus;
  performanceLabel: string;
  ratingAverage: number;
  ratingCount: number;
  complaintCount: number;
  jobsCompleted: number;
  avgTurnaroundDays: number | null;
  activeJobs: number;
  panelRating: number | null;
  flaggedAt: string | null;
}

export interface GaragePageResponse {
  content: GarageResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type GaragePerformanceStatus =
  | 'GOOD'
  | 'WATCH'
  | 'UNDERPERFORMING'
  | 'SUSPENDED';

export type RepairJobStatus =
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'IN_REPAIR'
  | 'AWAITING_PARTS'
  | 'QUALITY_CHECK'
  | 'COMPLETED'
  | 'DELIVERED'
  | 'CANCELLED';

export const GARAGE_PERFORMANCE_LABELS: Record<GaragePerformanceStatus, string> = {
  GOOD: 'Performing',
  WATCH: 'Under watch',
  UNDERPERFORMING: 'Underperforming',
  SUSPENDED: 'Suspended',
};

export const REPAIR_JOB_STATUS_LABELS: Record<RepairJobStatus, string> = {
  ASSIGNED: 'Assigned',
  ACCEPTED: 'Accepted',
  IN_REPAIR: 'In repair',
  AWAITING_PARTS: 'Awaiting parts',
  QUALITY_CHECK: 'Quality check',
  COMPLETED: 'Repair completed',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

export const REPAIR_JOB_PROGRESS: Record<RepairJobStatus, number> = {
  ASSIGNED: 5,
  ACCEPTED: 15,
  IN_REPAIR: 50,
  AWAITING_PARTS: 40,
  QUALITY_CHECK: 80,
  COMPLETED: 92,
  DELIVERED: 100,
  CANCELLED: 0,
};
