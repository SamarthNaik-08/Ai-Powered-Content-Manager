package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewriteContentRequest {

    @NotBlank(message = "Content to rewrite is required")
    private String content;

    private String tone; // e.g. Professional, Casual, Persuasive, Academic

    private String style; // e.g. Concise, Elaborate, Bullet-points, Storytelling

    private String instructions; // Additional specific instructions

    private Long contentId;
}
