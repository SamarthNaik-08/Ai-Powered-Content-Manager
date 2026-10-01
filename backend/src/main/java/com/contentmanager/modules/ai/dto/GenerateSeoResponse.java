package com.contentmanager.modules.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateSeoResponse {

    private String seoTitle;
    private String metaDescription;

    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    private String slugSuggestion;
}
