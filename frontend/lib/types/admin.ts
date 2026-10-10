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
  system: boolean;
  permissions: PermissionDefinition[];
}

export interface RoleCreateRequest {
  code: string;
  name: string;
  description?: string;
  permissionCodes?: string[];
}

export interface RoleUpdateRequest {
  name: string;
  description?: string;
  permissionCodes?: string[];
}

export interface CreateUserRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  nic?: string;
  role: string;
}

export interface UpdateUserRequest {
  fullName: string;
  phone?: string;
  nic?: string;
  role?: string;
  enabled?: boolean;
  password?: string;
}

export interface UserDetail {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  nic: string | null;
  role: string;
  roleName: string | null;
  enabled: boolean;
  initials: string;
  lastLoginAt: string | null;
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
