import { apiClient } from './apiClient';
import { ApiResponse, PageResponse } from '@/types/api';
import { MediaItem } from '@/types/enterprise';

export const mediaService = {
  uploadMedia: async (file: File, contentId?: number): Promise<MediaItem> => {
    const formData = new FormData();
    formData.append('file', file);
    if (contentId) {
      formData.append('contentId', contentId.toString());
    }

    const response = await apiClient.post<ApiResponse<MediaItem>>('/media', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  getMedia: async (params?: {
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<MediaItem>> => {
    const response = await apiClient.get<ApiResponse<PageResponse<MediaItem>>>('/media', {
      params: {
        keyword: params?.search,
        page: params?.page ?? 0,
        size: params?.size ?? 12,
      },
    });
    return response.data.data;
  },

  getMediaById: async (id: number): Promise<MediaItem> => {
    const response = await apiClient.get<ApiResponse<MediaItem>>(`/media/${id}`);
    return response.data.data;
  },

  deleteMedia: async (id: number): Promise<void> => {
    await apiClient.delete(`/media/${id}`);
  },
};
