package com.contentmanager.modules.ai.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzeContentResponse {

    private int readabilityScore; // 0 to 100
    private String readabilityGrade; // e.g. "Grade 8 - Very Good", "Difficult", etc.

    private int structureScore; // 0 to 100
    private String structureAnalysis; // Heading hierarchy, paragraph flow, bullet points

    private int seoCompletenessScore; // 0 to 100
    private String seoCompleteness; // Assessment of SEO elements

    private String toneConsistency; // Tone evaluation & consistency

    @Builder.Default
    private Map<String, Integer> keywordUsage = new HashMap<>(); // Keyword -> Frequency

    @Builder.Default
    private List<String> potentialRepetition = new ArrayList<>(); // Detected repetitive phrases

    @Builder.Default
    private List<String> missingSections = new ArrayList<>(); // Suggested missing sections

    @Builder.Default
    private List<String> recommendations = new ArrayList<>(); // Actionable steps to improve
}
