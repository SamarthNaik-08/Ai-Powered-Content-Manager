export type ContentType = 'BLOG_POST' | 'ARTICLE' | 'NEWS' | 'SOCIAL_POST' | 'DOCUMENT' | 'ANNOUNCEMENT';

export type ContentStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'SCHEDULED' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED';

export interface AuthorSummary {
  id: number;
  uuid: string;
  name: string;
  email: string;
}

export interface CategorySummary {
  id: number;
  name: string;
  slug: string;
}

export interface TagSummary {
  id: number;
  name: string;
  slug: string;
}

export interface Content {
  id: number;
  uuid: string;
  title: string;
  slug: string;
  description: string | null;
  body: string | null;
  excerpt: string | null;
  contentType: ContentType;
  status: ContentStatus;
  author: AuthorSummary;
  category: CategorySummary | null;
  tags: TagSummary[];
  featuredImage: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  publishedAt: string | null;
  scheduledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateContentPayload {
  title: string;
  description?: string;
  body?: string;
  excerpt?: string;
  contentType: ContentType;
  categoryId?: number | null;
  featuredImage?: string;
  seoTitle?: string;
  seoDescription?: string;
  scheduledAt?: string;
  tagIds?: number[];
}

export interface UpdateContentPayload {
  title?: string;
  description?: string;
  body?: string;
  excerpt?: string;
  contentType?: ContentType;
  categoryId?: number | null;
  featuredImage?: string;
  seoTitle?: string;
  seoDescription?: string;
  scheduledAt?: string;
  tagIds?: number[];
  changeSummary?: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContentVersion {
  id: number;
  contentId: number;
  versionNumber: number;
  title: string;
  body: string | null;
  changeSummary: string | null;
  createdByName: string;
  createdById: number;
  createdAt: string;
}

export interface DashboardStats {
  totalContent: number;
  drafts: number;
  inReview: number;
  approved: number;
  published: number;
  scheduled: number;
  archived: number;
  rejected: number;
  totalCategories: number;
  totalTags: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
