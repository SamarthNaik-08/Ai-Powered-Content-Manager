package com.contentmanager.modules.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateContentResponse {

    private String title;
    private String content;
    private String excerpt;

    @Builder.Default
    private List<String> suggestedTags = new ArrayList<>();
}
