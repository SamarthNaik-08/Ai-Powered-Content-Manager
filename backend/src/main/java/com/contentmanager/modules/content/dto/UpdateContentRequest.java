package com.contentmanager.modules.content.dto;

import com.contentmanager.modules.content.model.ContentType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentRequest {

    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String body;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    private ContentType contentType;

    private Long categoryId;

    @Size(max = 1000, message = "Featured image URL must not exceed 1000 characters")
    private String featuredImage;

    @Size(max = 300, message = "SEO title must not exceed 300 characters")
    private String seoTitle;

    @Size(max = 500, message = "SEO description must not exceed 500 characters")
    private String seoDescription;

    private Instant scheduledAt;

    private Set<Long> tagIds;

    @Size(max = 500, message = "Change summary must not exceed 500 characters")
    private String changeSummary;
}
