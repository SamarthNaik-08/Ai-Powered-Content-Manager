package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeContentRequest {

    @NotBlank(message = "Content to analyze is required")
    private String content;

    private String title;

    private Long contentId;
}
