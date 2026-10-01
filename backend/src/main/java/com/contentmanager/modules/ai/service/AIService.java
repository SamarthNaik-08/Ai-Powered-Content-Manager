package com.contentmanager.modules.ai.service;

import com.contentmanager.modules.ai.dto.*;
import com.contentmanager.modules.ai.exception.AIProviderException;
import com.contentmanager.modules.ai.exception.MalformedAIResponseException;
import com.contentmanager.modules.ai.model.*;
import com.contentmanager.modules.ai.provider.AIProvider;
import com.contentmanager.modules.ai.repository.AIRequestRepository;
import com.contentmanager.modules.ai.repository.AIResponseRepository;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class AIService {

    private final AIProvider aiProvider;
    private final AIUsageLimitService usageLimitService;
    private final AIRequestRepository aiRequestRepository;
    private final AIResponseRepository aiResponseRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;
    private final String configuredModel;
    private final com.contentmanager.modules.audit.service.AuditLogService auditLogService;

    public AIService(
            AIProvider aiProvider,
            AIUsageLimitService usageLimitService,
            AIRequestRepository aiRequestRepository,
            AIResponseRepository aiResponseRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            ObjectMapper objectMapper,
            com.contentmanager.modules.audit.service.AuditLogService auditLogService,
            @Value("${app.ai.model:gpt-4o-mini}") String configuredModel) {
        this.aiProvider = aiProvider;
        this.usageLimitService = usageLimitService;
        this.aiRequestRepository = aiRequestRepository;
        this.aiResponseRepository = aiResponseRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.configuredModel = configuredModel;
    }

    // =========================================================================
    // AI OPERATIONS
    // =========================================================================

    public GenerateContentResponse generate(GenerateContentRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.GENERATE,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.generate(request)
        );
    }

    public RewriteContentResponse rewrite(RewriteContentRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.REWRITE,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.rewrite(request)
        );
    }

    public SummarizeContentResponse summarize(SummarizeContentRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.SUMMARIZE,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.summarize(request)
        );
    }

    public GenerateTitlesResponse generateTitles(GenerateTitlesRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.TITLE_GEN,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.generateTitles(request)
        );
    }

    public GenerateSeoResponse generateSEO(GenerateSeoRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.SEO,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.generateSEO(request)
        );
    }

    public AnalyzeContentResponse analyze(AnalyzeContentRequest request, UserPrincipal principal) {
        return executeAiOperation(
                AIOperation.ANALYZE,
                request.getContentId(),
                principal,
                request,
                () -> aiProvider.analyze(request)
        );
    }

    public AIUsageResponse getUsage(UserPrincipal principal) {
        return usageLimitService.getUsageStatus(principal);
    }

    @Transactional(readOnly = true)
    public Page<AIHistoryItemResponse> getHistory(Long contentId, UserPrincipal principal, Pageable pageable) {
        Page<AIRequest> requests;
        if (contentId != null) {
            boolean isAdminOrEditor = principal != null && principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_EDITOR"));
            Content content = contentRepository.findById(contentId).orElse(null);
            boolean isOwner = content != null && content.getAuthor() != null &&
                    principal != null && content.getAuthor().getId().equals(principal.getId());

            if (!isAdminOrEditor && !isOwner) {
                // Scoped strictly to requesting user's own requests
                requests = aiRequestRepository.findByContentIdAndUserIdOrderByCreatedAtDesc(
                        contentId, principal != null ? principal.getId() : -1L, pageable);
            } else {
                requests = aiRequestRepository.findByContentIdOrderByCreatedAtDesc(contentId, pageable);
            }
        } else if (principal != null) {
            requests = aiRequestRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable);
        } else {
            requests = aiRequestRepository.findAll(pageable);
        }

        return requests.map(req -> {
            AIResponse resp = aiResponseRepository.findByRequestId(req.getId()).orElse(null);
            return AIHistoryItemResponse.builder()
                    .id(req.getId())
                    .uuid(req.getUuid())
                    .contentId(req.getContent() != null ? req.getContent().getId() : null)
                    .operation(req.getOperation())
                    .provider(req.getProvider())
                    .model(req.getModel())
                    .status(resp != null ? resp.getStatus() : null)
                    .executionTimeMs(resp != null ? resp.getExecutionTimeMs() : null)
                    .totalTokens(resp != null ? resp.getTotalTokens() : null)
                    .errorMessage(resp != null ? resp.getErrorMessage() : null)
                    .createdAt(req.getCreatedAt())
                    .build();
        });
    }

    // =========================================================================
    // EXECUTION & AUDITING PIPELINE
    // =========================================================================

    private <T> T executeAiOperation(
            AIOperation operation,
            Long contentId,
            UserPrincipal principal,
            Object requestDto,
            SupplierWithException<T> supplier) {

        // 1. Check rate limits
        usageLimitService.checkUsageLimit(principal);

        // 2. Resolve entities
        User user = (principal != null && principal.getId() != null)
                ? userRepository.findById(principal.getId()).orElse(null)
                : null;
        Content content = (contentId != null)
                ? contentRepository.findById(contentId).orElse(null)
                : null;

        // 3. Serialize metadata without any sensitive data
        String requestMetadata = serializeSafe(requestDto);

        // 4. Log AI Request
        AIRequest aiRequest = AIRequest.builder()
                .uuid(UUID.randomUUID())
                .user(user)
                .content(content)
                .operation(operation)
                .provider(aiProvider.getProviderName())
                .model(configuredModel)
                .requestMetadata(requestMetadata)
                .createdAt(Instant.now())
                .build();
        aiRequest = aiRequestRepository.save(aiRequest);

        long startTime = System.currentTimeMillis();
        try {
            // 5. Execute Provider call
            T result = supplier.get();
            long executionTimeMs = System.currentTimeMillis() - startTime;

            // 6. Log successful AI Response
            String responseMetadata = serializeSafe(result);
            AIResponse aiResponse = AIResponse.builder()
                    .uuid(UUID.randomUUID())
                    .request(aiRequest)
                    .status(AIStatus.SUCCESS)
                    .executionTimeMs(executionTimeMs)
                    .promptTokens(estimateTokens(requestMetadata))
                    .completionTokens(estimateTokens(responseMetadata))
                    .totalTokens(estimateTokens(requestMetadata) + estimateTokens(responseMetadata))
                    .responseMetadata(responseMetadata)
                    .createdAt(Instant.now())
                    .build();
            aiResponseRepository.save(aiResponse);

            auditLogService.logPrincipalAction(
                    principal,
                    com.contentmanager.modules.audit.model.AuditAction.AI_GENERATION,
                    "AI",
                    operation.name(),
                    "Operation " + operation.name() + " executed (" + executionTimeMs + "ms)"
            );

            log.info("AI operation {} succeeded for user {} in {}ms",
                    operation, principal != null ? principal.getEmail() : "system", executionTimeMs);

            return result;

        } catch (MalformedAIResponseException ex) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            recordFailedResponse(aiRequest, AIStatus.MALFORMED, executionTimeMs, ex.getMessage());
            log.error("AI operation {} returned malformed data: {}", operation, ex.getMessage());
            throw ex;

        } catch (AIProviderException ex) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            recordFailedResponse(aiRequest, AIStatus.FAILED, executionTimeMs, ex.getMessage());
            log.error("AI provider failure in operation {}: {}", operation, ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            recordFailedResponse(aiRequest, AIStatus.FAILED, executionTimeMs, ex.getMessage());
            log.error("Unexpected error in AI operation {}: {}", operation, ex.getMessage(), ex);
            throw new AIProviderException("An unexpected error occurred while communicating with the AI service: " + ex.getMessage());
        }
    }

    private void recordFailedResponse(AIRequest request, AIStatus status, long executionTimeMs, String errorMessage) {
        try {
            AIResponse failedResponse = AIResponse.builder()
                    .uuid(UUID.randomUUID())
                    .request(request)
                    .status(status)
                    .executionTimeMs(executionTimeMs)
                    .errorMessage(errorMessage)
                    .createdAt(Instant.now())
                    .build();
            aiResponseRepository.save(failedResponse);
        } catch (Exception e) {
            log.error("Failed to save AI failure response record: {}", e.getMessage());
        }
    }

    private String serializeSafe(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Serialization failed\"}";
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        // Simple token estimate: ~4 chars per token
        return Math.max(1, text.length() / 4);
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
