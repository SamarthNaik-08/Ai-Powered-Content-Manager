import { apiClient } from './apiClient';
import { ApiResponse } from '@/types/api';
import { AnalyticsDashboardData, AIAnalyticsData } from '@/types/enterprise';

export const analyticsService = {
  getDashboardAnalytics: async (): Promise<AnalyticsDashboardData> => {
    const response = await apiClient.get<ApiResponse<AnalyticsDashboardData>>('/analytics/dashboard');
    return response.data.data;
  },

  getAIAnalytics: async (): Promise<AIAnalyticsData> => {
    const response = await apiClient.get<ApiResponse<AIAnalyticsData>>('/analytics/ai');
    return response.data.data;
  },
};
