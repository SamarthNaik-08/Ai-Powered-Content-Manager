import { apiClient } from './apiClient';
import { ApiResponse } from '@/types/api';
import {
  GenerateContentRequest,
  GenerateContentResponse,
  RewriteContentRequest,
  RewriteContentResponse,
  SummarizeContentRequest,
  SummarizeContentResponse,
  GenerateTitlesRequest,
  GenerateTitlesResponse,
  GenerateSeoRequest,
  GenerateSeoResponse,
  AnalyzeContentRequest,
  AnalyzeContentResponse,
  AIUsageResponse,
  AIHistoryItem,
} from '@/types/ai';
import { PageResponse } from '@/types/content';

export const aiService = {
  generate: async (data: GenerateContentRequest): Promise<GenerateContentResponse> => {
    const res = await apiClient.post<ApiResponse<GenerateContentResponse>>('/ai/generate', data);
    return res.data.data!;
  },

  rewrite: async (data: RewriteContentRequest): Promise<RewriteContentResponse> => {
    const res = await apiClient.post<ApiResponse<RewriteContentResponse>>('/ai/rewrite', data);
    return res.data.data!;
  },

  summarize: async (data: SummarizeContentRequest): Promise<SummarizeContentResponse> => {
    const res = await apiClient.post<ApiResponse<SummarizeContentResponse>>('/ai/summarize', data);
    return res.data.data!;
  },

  generateTitles: async (data: GenerateTitlesRequest): Promise<GenerateTitlesResponse> => {
    const res = await apiClient.post<ApiResponse<GenerateTitlesResponse>>('/ai/titles', data);
    return res.data.data!;
  },

  generateSEO: async (data: GenerateSeoRequest): Promise<GenerateSeoResponse> => {
    const res = await apiClient.post<ApiResponse<GenerateSeoResponse>>('/ai/seo', data);
    return res.data.data!;
  },

  analyze: async (data: AnalyzeContentRequest): Promise<AnalyzeContentResponse> => {
    const res = await apiClient.post<ApiResponse<AnalyzeContentResponse>>('/ai/analyze', data);
    return res.data.data!;
  },

  getUsage: async (): Promise<AIUsageResponse> => {
    const res = await apiClient.get<ApiResponse<AIUsageResponse>>('/ai/usage');
    return res.data.data!;
  },

  getHistory: async (contentId?: number, page = 0, size = 10): Promise<PageResponse<AIHistoryItem>> => {
    const params = new URLSearchParams();
    if (contentId) params.append('contentId', contentId.toString());
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await apiClient.get<ApiResponse<PageResponse<AIHistoryItem>>>(`/ai/history?${params.toString()}`);
    return res.data.data!;
  },
};
