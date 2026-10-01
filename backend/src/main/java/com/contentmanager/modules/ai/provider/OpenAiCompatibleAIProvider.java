package com.contentmanager.modules.ai.provider;

import com.contentmanager.modules.ai.dto.*;
import com.contentmanager.modules.ai.exception.AIProviderException;
import com.contentmanager.modules.ai.exception.MalformedAIResponseException;
import com.contentmanager.modules.ai.service.PromptTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component("openAiCompatibleAIProvider")
public class OpenAiCompatibleAIProvider implements AIProvider {

    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public OpenAiCompatibleAIProvider(
            PromptTemplateService promptTemplateService,
            ObjectMapper objectMapper,
            @Value("${app.ai.api-url:https://api.openai.com/v1}") String apiUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.timeout-seconds:30}") int timeoutSeconds) {

        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public GenerateContentResponse generate(GenerateContentRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForGeneration();
        String userPrompt = promptTemplateService.renderGenerationPrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, GenerateContentResponse.class);
    }

    @Override
    public RewriteContentResponse rewrite(RewriteContentRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForRewrite();
        String userPrompt = promptTemplateService.renderRewritePrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, RewriteContentResponse.class);
    }

    @Override
    public SummarizeContentResponse summarize(SummarizeContentRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForSummarize();
        String userPrompt = promptTemplateService.renderSummarizePrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, SummarizeContentResponse.class);
    }

    @Override
    public GenerateTitlesResponse generateTitles(GenerateTitlesRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForTitles();
        String userPrompt = promptTemplateService.renderTitlesPrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, GenerateTitlesResponse.class);
    }

    @Override
    public GenerateSeoResponse generateSEO(GenerateSeoRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForSeo();
        String userPrompt = promptTemplateService.renderSeoPrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, GenerateSeoResponse.class);
    }

    @Override
    public AnalyzeContentResponse analyze(AnalyzeContentRequest request) {
        String systemPrompt = promptTemplateService.getSystemPromptForAnalyze();
        String userPrompt = promptTemplateService.renderAnalyzePrompt(request);
        String json = executeChatCompletion(systemPrompt, userPrompt);
        return parseResponse(json, AnalyzeContentResponse.class);
    }

    // =========================================================================
    // HTTP EXECUTION & JSON CLEANUP
    // =========================================================================

    private String executeChatCompletion(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIProviderException("AI API key is not configured for OpenAI-compatible provider.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", model);
        requestPayload.put("temperature", 0.7);
        requestPayload.put("response_format", Map.of("type", "json_object"));

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
        requestPayload.put("messages", messages);

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new MalformedAIResponseException("Empty response received from AI provider.");
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (choices.isEmpty() || !choices.isArray()) {
                throw new MalformedAIResponseException("Malformed response from AI provider: 'choices' array missing.");
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new MalformedAIResponseException("Empty content received in AI completion.");
            }

            return cleanJson(content);

        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("AI Provider rate limit encountered: {}", ex.getMessage());
            throw new AIProviderException("Upstream AI provider rate limit exceeded. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("AI Provider HTTP error [{}]: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AIProviderException("AI Provider communication error: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException ex) {
            log.error("AI Provider connection timed out: {}", ex.getMessage());
            throw new AIProviderException("AI provider request timed out. Please try again.", HttpStatus.GATEWAY_TIMEOUT);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse AI provider envelope: {}", ex.getMessage());
            throw new MalformedAIResponseException("Unable to parse AI response payload.");
        }
    }

    private String cleanJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private <T> T parseResponse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse structured JSON into {}: {}", clazz.getSimpleName(), e.getMessage());
            throw new MalformedAIResponseException("AI response could not be parsed into the required structured format.");
        }
    }
}
