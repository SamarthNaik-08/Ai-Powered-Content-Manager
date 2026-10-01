import { apiClient } from './apiClient';
import { ApiResponse, PageResponse } from '@/types/api';
import { UserManagementItem, UserStatus } from '@/types/enterprise';

export const userAdminService = {
  getUsers: async (params?: {
    search?: string;
    status?: UserStatus;
    role?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<UserManagementItem>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<UserManagementItem>>>('/users', {
      params: {
        search: params?.search,
        status: params?.status,
        role: params?.role,
        page: params?.page ?? 0,
        size: params?.size ?? 15,
      },
    });
    return response.data.data;
  },

  getUserById: async (id: number): Promise<UserManagementItem> => {
    const response = await apiClient.get<ApiResponse<UserManagementItem>>(`/users/${id}`);
    return response.data.data;
  },

  updateRoles: async (id: number, roles: string[]): Promise<UserManagementItem> => {
    const response = await apiClient.put<ApiResponse<UserManagementItem>>(`/users/${id}/roles`, {
      roles,
    });
    return response.data.data;
  },

  updateStatus: async (id: number, status: UserStatus): Promise<UserManagementItem> => {
    const response = await apiClient.put<ApiResponse<UserManagementItem>>(`/users/${id}/status`, {
      status,
    });
    return response.data.data;
  },
};
