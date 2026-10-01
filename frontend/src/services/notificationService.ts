import { apiClient } from './apiClient';
import { ApiResponse, PageResponse } from '@/types/api';
import { NotificationItem } from '@/types/enterprise';

export const notificationService = {
  getNotifications: async (
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<NotificationItem>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<NotificationItem>>>('/notifications', {
      params: { page, size },
    });
    return response.data.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<ApiResponse<{ unreadCount?: number } | number>>('/notifications/unread-count');
    const data = response.data.data;
    if (typeof data === 'number') {
      return data;
    }
    return data?.unreadCount ?? 0;
  },

  markAsRead: async (id: number): Promise<NotificationItem> => {
    const response = await apiClient.patch<ApiResponse<NotificationItem>>(`/notifications/${id}/read`);
    return response.data.data;
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.post('/notifications/mark-all-read');
  },
};
