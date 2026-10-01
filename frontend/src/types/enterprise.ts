export interface MediaItem {
  id: number;
  uuid: string;
  filename: string;
  originalFilename: string;
  mimeType: string;
  size: number;
  storageUrl: string;
  uploadedBy?: {
    id: number;
    name: string;
    email: string;
  };
  contentId?: number;
  createdAt: string;
}

export type NotificationType =
  | 'REVIEW_REQUESTED'
  | 'CONTENT_APPROVED'
  | 'CONTENT_REJECTED'
  | 'CONTENT_PUBLISHED'
  | 'SCHEDULED_PUBLISHED'
  | 'SYSTEM_EVENT';

export interface NotificationItem {
  id: number;
  uuid: string;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  link?: string;
  createdAt: string;
}

export type AuditAction =
  | 'LOGIN'
  | 'LOGOUT'
  | 'CREATE_CONTENT'
  | 'UPDATE_CONTENT'
  | 'DELETE_CONTENT'
  | 'SUBMIT_CONTENT'
  | 'APPROVE_CONTENT'
  | 'REJECT_CONTENT'
  | 'PUBLISH_CONTENT'
  | 'SCHEDULE_CONTENT'
  | 'RESTORE_VERSION'
  | 'AI_GENERATION'
  | 'USER_CREATED'
  | 'ROLE_CHANGED'
  | 'USER_STATUS_UPDATED'
  | 'MEDIA_UPLOAD'
  | 'MEDIA_DELETE';

export interface AuditLogItem {
  id: number;
  uuid: string;
  actorId?: number;
  actorEmail: string;
  action: AuditAction;
  resource?: string;
  resourceId?: string;
  ipAddress?: string;
  userAgent?: string;
  metadata?: string;
  createdAt: string;
}

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface UserManagementItem {
  id: number;
  uuid: string;
  name: string;
  email: string;
  status: UserStatus;
  roles: string[];
  permissions: string[];
  createdAt: string;
  lastLoginAt?: string;
}

export interface ContentCounts {
  total: number;
  published: number;
  draft: number;
  inReview: number;
  approved: number;
  scheduled: number;
  rejected: number;
  archived: number;
}

export interface DistributionItem {
  name: string;
  count: number;
}

export interface TrendPoint {
  date: string;
  count: number;
}

export interface AnalyticsDashboardData {
  counts: ContentCounts;
  byCategory: DistributionItem[];
  byType: DistributionItem[];
  byAuthor: DistributionItem[];
  creationTrend: TrendPoint[];
  publishingTrend: TrendPoint[];
}

export interface AIAnalyticsData {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  totalTokens: number;
  averageExecutionTimeMs: number;
  byOperation: Record<string, number>;
  byProvider: Record<string, number>;
  byModel: Record<string, number>;
}
