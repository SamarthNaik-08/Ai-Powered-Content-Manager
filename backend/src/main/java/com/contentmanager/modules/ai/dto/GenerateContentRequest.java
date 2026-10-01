package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateContentRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    private String contentType; // e.g. BLOG_POST, ARTICLE, NEWS, SOCIAL_POST

    private String tone; // e.g. Professional, Casual, Academic, Engaging

    private String length; // SHORT, MEDIUM, LONG

    private String audience; // General, Developers, Executives, Marketers

    private String instructions; // Additional specific instructions

    private Long contentId;
}
