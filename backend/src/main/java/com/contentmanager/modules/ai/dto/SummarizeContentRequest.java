package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummarizeContentRequest {

    @NotBlank(message = "Content to summarize is required")
    private String content;

    private Long contentId;
}
