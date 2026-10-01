package com.contentmanager.modules.ai.provider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class AIProviderFactory implements AIProvider {

    private final AIProvider mockProvider;
    private final AIProvider openAiProvider;
    private final String configuredProvider;
    private final String apiKey;

    public AIProviderFactory(
            @Qualifier("mockAIProvider") AIProvider mockProvider,
            @Qualifier("openAiCompatibleAIProvider") AIProvider openAiProvider,
            @Value("${app.ai.provider:mock}") String configuredProvider,
            @Value("${app.ai.api-key:}") String apiKey) {
        this.mockProvider = mockProvider;
        this.openAiProvider = openAiProvider;
        this.configuredProvider = configuredProvider;
        this.apiKey = apiKey;
    }

    public AIProvider getActiveProvider() {
        if ("openai".equalsIgnoreCase(configuredProvider) && apiKey != null && !apiKey.isBlank()) {
            return openAiProvider;
        }
        return mockProvider;
    }

    @Override
    public String getProviderName() {
        return getActiveProvider().getProviderName();
    }

    @Override
    public com.contentmanager.modules.ai.dto.GenerateContentResponse generate(com.contentmanager.modules.ai.dto.GenerateContentRequest request) {
        return getActiveProvider().generate(request);
    }

    @Override
    public com.contentmanager.modules.ai.dto.RewriteContentResponse rewrite(com.contentmanager.modules.ai.dto.RewriteContentRequest request) {
        return getActiveProvider().rewrite(request);
    }

    @Override
    public com.contentmanager.modules.ai.dto.SummarizeContentResponse summarize(com.contentmanager.modules.ai.dto.SummarizeContentRequest request) {
        return getActiveProvider().summarize(request);
    }

    @Override
    public com.contentmanager.modules.ai.dto.GenerateTitlesResponse generateTitles(com.contentmanager.modules.ai.dto.GenerateTitlesRequest request) {
        return getActiveProvider().generateTitles(request);
    }

    @Override
    public com.contentmanager.modules.ai.dto.GenerateSeoResponse generateSEO(com.contentmanager.modules.ai.dto.GenerateSeoRequest request) {
        return getActiveProvider().generateSEO(request);
    }

    @Override
    public com.contentmanager.modules.ai.dto.AnalyzeContentResponse analyze(com.contentmanager.modules.ai.dto.AnalyzeContentRequest request) {
        return getActiveProvider().analyze(request);
    }
}
