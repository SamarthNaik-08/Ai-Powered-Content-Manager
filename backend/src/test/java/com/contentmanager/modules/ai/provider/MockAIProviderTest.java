package com.contentmanager.modules.ai.provider;

import com.contentmanager.modules.ai.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockAIProvider Tests")
class MockAIProviderTest {

    private MockAIProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockAIProvider();
    }

    @Test
    @DisplayName("generate returns structured content, title, excerpt, and tags")
    void generate_success() {
        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Cloud Architecture")
                .contentType("ARTICLE")
                .tone("Professional")
                .length("MEDIUM")
                .audience("Developers")
                .build();

        GenerateContentResponse response = provider.generate(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).contains("Cloud Architecture");
        assertThat(response.getContent()).contains("# The Ultimate Guide to Cloud Architecture");
        assertThat(response.getContent()).contains("## 1. Why Cloud Architecture Matters Now");
        assertThat(response.getExcerpt()).contains("Cloud Architecture");
        assertThat(response.getSuggestedTags()).isNotEmpty();
    }

    @Test
    @DisplayName("rewrite modifies content applying tone and style")
    void rewrite_success() {
        RewriteContentRequest request = RewriteContentRequest.builder()
                .content("Original draft text that needs improvement.")
                .tone("Persuasive")
                .style("Concise")
                .build();

        RewriteContentResponse response = provider.rewrite(request);

        assertThat(response).isNotNull();
        assertThat(response.getRewrittenContent()).contains("Original draft text");
        assertThat(response.getRewrittenContent()).contains("Persuasive");
    }

    @Test
    @DisplayName("summarize returns summary and key takeaways")
    void summarize_success() {
        SummarizeContentRequest request = SummarizeContentRequest.builder()
                .content("Here is a long text discussing software engineering design principles.")
                .build();

        SummarizeContentResponse response = provider.summarize(request);

        assertThat(response).isNotNull();
        assertThat(response.getSummary()).isNotBlank();
        assertThat(response.getKeyPoints()).hasSize(4);
    }

    @Test
    @DisplayName("generateTitles returns distinct title suggestions")
    void generateTitles_success() {
        GenerateTitlesRequest request = GenerateTitlesRequest.builder()
                .topicOrContent("Microservices Scalability")
                .count(5)
                .build();

        GenerateTitlesResponse response = provider.generateTitles(request);

        assertThat(response).isNotNull();
        assertThat(response.getTitles()).hasSize(5);
        assertThat(response.getTitles().get(0)).contains("Microservices Scalability");
    }

    @Test
    @DisplayName("generateSEO returns seoTitle, metaDescription, keywords, and slug")
    void generateSEO_success() {
        GenerateSeoRequest request = GenerateSeoRequest.builder()
                .content("Detailed content about Spring Boot microservices and REST APIs.")
                .title("Spring Boot Microservices Guide")
                .excerpt("A complete guide to Spring Boot microservices.")
                .build();

        GenerateSeoResponse response = provider.generateSEO(request);

        assertThat(response).isNotNull();
        assertThat(response.getSeoTitle()).isEqualTo("Spring Boot Microservices Guide");
        assertThat(response.getMetaDescription()).contains("Spring Boot");
        assertThat(response.getSlugSuggestion()).isEqualTo("spring-boot-microservices-guide");
        assertThat(response.getKeywords()).isNotEmpty();
    }

    @Test
    @DisplayName("analyze returns full metrics including readability, structure, and recommendations")
    void analyze_success() {
        AnalyzeContentRequest request = AnalyzeContentRequest.builder()
                .content("# Headings\n\nThis is a well structured article discussing software architecture with sufficient details and key takeaways.")
                .title("Software Architecture")
                .build();

        AnalyzeContentResponse response = provider.analyze(request);

        assertThat(response).isNotNull();
        assertThat(response.getReadabilityScore()).isBetween(0, 100);
        assertThat(response.getReadabilityGrade()).isNotBlank();
        assertThat(response.getStructureScore()).isBetween(0, 100);
        assertThat(response.getSeoCompletenessScore()).isBetween(0, 100);
        assertThat(response.getRecommendations()).isNotEmpty();
    }
}
