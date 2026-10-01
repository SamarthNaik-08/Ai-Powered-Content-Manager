export type AIOperation = 'GENERATE' | 'REWRITE' | 'SUMMARIZE' | 'TITLE_GEN' | 'SEO' | 'ANALYZE';

export type AIStatus = 'SUCCESS' | 'FAILED' | 'MALFORMED' | 'RATE_LIMITED';

export interface GenerateContentRequest {
  topic: string;
  contentType?: string;
  tone?: string;
  length?: 'SHORT' | 'MEDIUM' | 'LONG';
  audience?: string;
  instructions?: string;
  contentId?: number;
}

export interface GenerateContentResponse {
  title: string;
  content: string;
  excerpt: string;
  suggestedTags: string[];
}

export interface RewriteContentRequest {
  content: string;
  tone?: string;
  style?: string;
  instructions?: string;
  contentId?: number;
}

export interface RewriteContentResponse {
  rewrittenContent: string;
}

export interface SummarizeContentRequest {
  content: string;
  contentId?: number;
}

export interface SummarizeContentResponse {
  summary: string;
  keyPoints: string[];
}

export interface GenerateTitlesRequest {
  topicOrContent: string;
  count?: number;
  contentId?: number;
}

export interface GenerateTitlesResponse {
  titles: string[];
}

export interface GenerateSeoRequest {
  content: string;
  title?: string;
  excerpt?: string;
  contentId?: number;
}

export interface GenerateSeoResponse {
  seoTitle: string;
  metaDescription: string;
  keywords: string[];
  slugSuggestion: string;
}

export interface AnalyzeContentRequest {
  content: string;
  title?: string;
  contentId?: number;
}

export interface AnalyzeContentResponse {
  readabilityScore: number;
  readabilityGrade: string;
  structureScore: number;
  structureAnalysis: string;
  seoCompletenessScore: number;
  seoCompleteness: string;
  toneConsistency: string;
  keywordUsage: Record<string, number>;
  potentialRepetition: string[];
  missingSections: string[];
  recommendations: string[];
}

export interface AIUsageResponse {
  usedToday: number;
  dailyLimit: number;
  remainingToday: number;
  role: string;
}

export interface AIHistoryItem {
  id: number;
  uuid: string;
  contentId?: number;
  operation: AIOperation;
  provider: string;
  model: string;
  status: AIStatus;
  totalTokens?: number;
  executionTimeMs?: number;
  errorMessage?: string;
  createdAt: string;
}
