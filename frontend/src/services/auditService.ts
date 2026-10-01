import { apiClient } from './apiClient';
import { ApiResponse, PageResponse } from '@/types/api';
import { AuditLogItem } from '@/types/enterprise';

export const auditService = {
  getAuditLogs: async (params?: {
    actorEmail?: string;
    action?: string;
    resource?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<AuditLogItem>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<AuditLogItem>>>('/audit-logs', {
      params: {
        actorEmail: params?.actorEmail,
        action: params?.action,
        resource: params?.resource,
        from: params?.from,
        to: params?.to,
        page: params?.page ?? 0,
        size: params?.size ?? 20,
      },
    });
    return response.data.data;
  },
};
