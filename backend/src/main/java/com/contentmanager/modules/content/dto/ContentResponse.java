package com.contentmanager.modules.content.dto;

import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private Long id;
    private UUID uuid;
    private String title;
    private String slug;
    private String description;
    private String body;
    private String excerpt;
    private ContentType contentType;
    private ContentStatus status;
    private AuthorSummary author;
    private CategorySummary category;
    private Set<TagSummary> tags;
    private String featuredImage;
    private String seoTitle;
    private String seoDescription;
    private Instant publishedAt;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorSummary {
        private Long id;
        private UUID uuid;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagSummary {
        private Long id;
        private String name;
        private String slug;
    }
}
