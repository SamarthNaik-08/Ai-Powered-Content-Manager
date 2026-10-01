package com.contentmanager.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateTitlesRequest {

    @NotBlank(message = "Content or topic is required")
    private String topicOrContent;

    @Builder.Default
    private Integer count = 5;

    private Long contentId;
}
