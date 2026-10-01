import { apiClient } from './apiClient';
import { ApiResponse } from '@/types/api';
import {
  Content,
  CreateContentPayload,
  UpdateContentPayload,
  Category,
  Tag,
  ContentVersion,
  DashboardStats,
  PageResponse,
  ContentStatus,
  ContentType,
} from '@/types/content';

export interface ContentSearchParams {
  keyword?: string;
  status?: ContentStatus;
  contentType?: ContentType;
  authorId?: number;
  categoryId?: number;
  tagId?: number;
  createdAfter?: string;
  createdBefore?: string;
  publishedAfter?: string;
  publishedBefore?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export const contentService = {
  // ========================= CONTENT CRUD =========================

  async searchContents(params: ContentSearchParams = {}): Promise<PageResponse<Content>> {
    const response = await apiClient.get<ApiResponse<PageResponse<Content>>>('/contents', { params });
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch contents');
    }
    return response.data.data;
  },

  async getContentById(id: number): Promise<Content> {
    const response = await apiClient.get<ApiResponse<Content>>(`/contents/${id}`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch content');
    }
    return response.data.data;
  },

  async createContent(payload: CreateContentPayload): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>('/contents', payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to create content');
    }
    return response.data.data;
  },

  async updateContent(id: number, payload: UpdateContentPayload): Promise<Content> {
    const response = await apiClient.put<ApiResponse<Content>>(`/contents/${id}`, payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to update content');
    }
    return response.data.data;
  },

  async deleteContent(id: number): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(`/contents/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete content');
    }
  },

  // ========================= WORKFLOW =========================

  async submitForReview(id: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/submit`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to submit content');
    }
    return response.data.data;
  },

  async approve(id: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/approve`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to approve content');
    }
    return response.data.data;
  },

  async reject(id: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/reject`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to reject content');
    }
    return response.data.data;
  },

  async publish(id: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/publish`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to publish content');
    }
    return response.data.data;
  },

  async schedule(id: number, scheduledAt: string): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/schedule`, {
      scheduledAt,
    });
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to schedule content');
    }
    return response.data.data;
  },

  async archive(id: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(`/contents/${id}/archive`);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to archive content');
    }
    return response.data.data;
  },

  // ========================= VERSIONING =========================

  async getVersions(contentId: number, page = 0, size = 20): Promise<PageResponse<ContentVersion>> {
    const response = await apiClient.get<ApiResponse<PageResponse<ContentVersion>>>(
      `/contents/${contentId}/versions`,
      { params: { page, size } }
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch versions');
    }
    return response.data.data;
  },

  async getVersion(contentId: number, versionNumber: number): Promise<ContentVersion> {
    const response = await apiClient.get<ApiResponse<ContentVersion>>(
      `/contents/${contentId}/versions/${versionNumber}`
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch version');
    }
    return response.data.data;
  },

  async restoreVersion(contentId: number, versionNumber: number): Promise<Content> {
    const response = await apiClient.post<ApiResponse<Content>>(
      `/contents/${contentId}/versions/${versionNumber}/restore`
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to restore version');
    }
    return response.data.data;
  },

  // ========================= DASHBOARD =========================

  async getDashboardStats(): Promise<DashboardStats> {
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/contents/stats/dashboard');
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch stats');
    }
    return response.data.data;
  },

  // ========================= CATEGORIES =========================

  async getCategories(): Promise<Category[]> {
    const response = await apiClient.get<ApiResponse<Category[]>>('/categories');
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch categories');
    }
    return response.data.data;
  },

  async createCategory(payload: { name: string; description?: string }): Promise<Category> {
    const response = await apiClient.post<ApiResponse<Category>>('/categories', payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to create category');
    }
    return response.data.data;
  },

  async updateCategory(id: number, payload: { name?: string; description?: string }): Promise<Category> {
    const response = await apiClient.put<ApiResponse<Category>>(`/categories/${id}`, payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to update category');
    }
    return response.data.data;
  },

  async deleteCategory(id: number): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(`/categories/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete category');
    }
  },

  // ========================= TAGS =========================

  async getTags(): Promise<Tag[]> {
    const response = await apiClient.get<ApiResponse<Tag[]>>('/tags');
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to fetch tags');
    }
    return response.data.data;
  },

  async createTag(payload: { name: string }): Promise<Tag> {
    const response = await apiClient.post<ApiResponse<Tag>>('/tags', payload);
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || 'Failed to create tag');
    }
    return response.data.data;
  },

  async deleteTag(id: number): Promise<void> {
    const response = await apiClient.delete<ApiResponse<void>>(`/tags/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete tag');
    }
  },
};
