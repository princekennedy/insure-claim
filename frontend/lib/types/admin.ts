export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
  lastLoginAt: string | null;
}

export interface PermissionDefinition {
  code: string;
  name: string;
  description: string;
}

export interface RoleDefinition {
  code: string;
  name: string;
  description: string;
  permissions: PermissionDefinition[];
}

export interface AuditLogResponse {
  id: number;
  actorId: number | null;
  actorEmail: string | null;
  actorRole: string | null;
  action: string;
  description: string;
  entityType: string | null;
  entityId: string | null;
  requestMethod: string;
  requestPath: string;
  status: number;
  success: boolean;
  detail: string | null;
  ipAddress: string | null;
  createdAt: string;
}
