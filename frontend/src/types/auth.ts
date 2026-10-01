export type RoleName = 'ROLE_ADMIN' | 'ROLE_EDITOR' | 'ROLE_AUTHOR' | 'ROLE_VIEWER';

export type PermissionName =
  | 'CONTENT_CREATE'
  | 'CONTENT_READ'
  | 'CONTENT_UPDATE'
  | 'CONTENT_DELETE'
  | 'CONTENT_SUBMIT'
  | 'CONTENT_APPROVE'
  | 'CONTENT_PUBLISH'
  | 'CONTENT_SCHEDULE'
  | 'AI_GENERATE'
  | 'AI_REWRITE'
  | 'AI_SUMMARIZE'
  | 'AI_SEO'
  | 'AI_ANALYZE'
  | 'USER_MANAGE'
  | 'ANALYTICS_VIEW'
  | 'AUDIT_VIEW'
  | 'MEDIA_MANAGE'
  | 'SETTINGS_MANAGE';

export interface User {
  id: number;
  uuid: string;
  name: string;
  email: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  roles: RoleName[];
  permissions: PermissionName[];
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}
