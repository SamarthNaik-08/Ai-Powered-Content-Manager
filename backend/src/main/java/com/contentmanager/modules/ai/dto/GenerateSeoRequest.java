package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateSeoRequest {

    @NotBlank(message = "Content is required")
    private String content;

    private String title;

    private String excerpt;

    private Long contentId;
}
