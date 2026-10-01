package com.contentmanager.modules.ai.provider;

import com.contentmanager.modules.ai.dto.GenerateContentRequest;
import com.contentmanager.modules.ai.exception.AIProviderException;
import com.contentmanager.modules.ai.service.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OpenAiCompatibleAIProvider Tests")
class OpenAiCompatibleAIProviderTest {

    private PromptTemplateService promptTemplateService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        promptTemplateService = new PromptTemplateService();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("throws AIProviderException if API key is missing")
    void missingApiKey_throwsAIProviderException() {
        OpenAiCompatibleAIProvider provider = new OpenAiCompatibleAIProvider(
                promptTemplateService,
                objectMapper,
                "https://api.openai.com/v1",
                "", // empty API key
                "gpt-4o-mini",
                30
        );

        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Cloud")
                .build();

        assertThatThrownBy(() -> provider.generate(request))
                .isInstanceOf(AIProviderException.class)
                .hasMessageContaining("AI API key is not configured");
    }

    @Test
    @DisplayName("getProviderName returns OPENAI")
    void getProviderName_returnsOpenAi() {
        OpenAiCompatibleAIProvider provider = new OpenAiCompatibleAIProvider(
                promptTemplateService,
                objectMapper,
                "https://api.openai.com/v1",
                "test-key",
                "gpt-4o-mini",
                30
        );

        assertThat(provider.getProviderName()).isEqualTo("OPENAI");
    }
}
