package com.contentmanager.modules.ai.service;

import com.contentmanager.modules.ai.dto.*;
import com.contentmanager.modules.ai.exception.AIProviderException;
import com.contentmanager.modules.ai.exception.AIRateLimitExceededException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIService Tests")
class AIServiceTest {

    @Mock private AIProvider aiProvider;
    @Mock private AIUsageLimitService usageLimitService;
    @Mock private AIRequestRepository aiRequestRepository;
    @Mock private AIResponseRepository aiResponseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private com.contentmanager.modules.audit.service.AuditLogService auditLogService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private AIService aiService;

    private UserPrincipal testPrincipal;
    private User testUser;
    private Content testContent;

    @BeforeEach
    void setUp() {
        aiService = new AIService(
                aiProvider,
                usageLimitService,
                aiRequestRepository,
                aiResponseRepository,
                userRepository,
                contentRepository,
                objectMapper,
                auditLogService,
                "gpt-4o-mini"
        );

        testPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("editor@test.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_EDITOR"), new SimpleGrantedAuthority("AI_GENERATE")))
                .build();

        testUser = User.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .email("editor@test.com")
                .name("Editor User")
                .build();

        testContent = Content.builder()
                .id(100L)
                .title("Original Title")
                .body("Original Body")
                .build();
    }

    // =========================================================================
    // GENERATE & CONTENT SAFETY
    // =========================================================================

    @Test
    @DisplayName("generate returns generated content, logs request/response, and never modifies content repository")
    void generate_success() {
        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Microservices")
                .contentId(100L)
                .build();

        GenerateContentResponse mockResponse = GenerateContentResponse.builder()
                .title("Microservices Mastery")
                .content("AI Generated Body")
                .excerpt("AI Excerpt")
                .suggestedTags(List.of("microservices", "cloud"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(contentRepository.findById(100L)).thenReturn(Optional.of(testContent));
        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.generate(request)).thenReturn(mockResponse);
        when(aiRequestRepository.save(any(AIRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateContentResponse result = aiService.generate(request, testPrincipal);

        assertThat(result.getTitle()).isEqualTo("Microservices Mastery");
        assertThat(result.getContent()).isEqualTo("AI Generated Body");

        // Verify safety: contentRepository.save must NEVER be called
        verify(contentRepository, never()).save(any());

        // Verify request logged
        ArgumentCaptor<AIRequest> reqCaptor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiRequestRepository).save(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getOperation()).isEqualTo(AIOperation.GENERATE);
        assertThat(reqCaptor.getValue().getUser()).isEqualTo(testUser);

        // Verify response logged
        ArgumentCaptor<AIResponse> respCaptor = ArgumentCaptor.forClass(AIResponse.class);
        verify(aiResponseRepository).save(respCaptor.capture());
        assertThat(respCaptor.getValue().getStatus()).isEqualTo(AIStatus.SUCCESS);
        assertThat(respCaptor.getValue().getExecutionTimeMs()).isNotNull();
    }

    // =========================================================================
    // REWRITE
    // =========================================================================

    @Test
    @DisplayName("rewrite invokes provider and tracks success")
    void rewrite_success() {
        RewriteContentRequest request = RewriteContentRequest.builder()
                .content("Draft text")
                .tone("Persuasive")
                .build();

        RewriteContentResponse mockResp = RewriteContentResponse.builder()
                .rewrittenContent("Persuasive draft text")
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.rewrite(request)).thenReturn(mockResp);
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RewriteContentResponse result = aiService.rewrite(request, testPrincipal);

        assertThat(result.getRewrittenContent()).isEqualTo("Persuasive draft text");
        verify(aiProvider).rewrite(request);
    }

    // =========================================================================
    // SUMMARIZE
    // =========================================================================

    @Test
    @DisplayName("summarize invokes provider and returns summary")
    void summarize_success() {
        SummarizeContentRequest request = SummarizeContentRequest.builder()
                .content("Long article content")
                .build();

        SummarizeContentResponse mockResp = SummarizeContentResponse.builder()
                .summary("Article summary")
                .keyPoints(List.of("Point 1", "Point 2"))
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.summarize(request)).thenReturn(mockResp);
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SummarizeContentResponse result = aiService.summarize(request, testPrincipal);

        assertThat(result.getSummary()).isEqualTo("Article summary");
        assertThat(result.getKeyPoints()).hasSize(2);
    }

    // =========================================================================
    // TITLES & SEO & ANALYZE
    // =========================================================================

    @Test
    @DisplayName("generateTitles returns title list")
    void generateTitles_success() {
        GenerateTitlesRequest request = GenerateTitlesRequest.builder()
                .topicOrContent("AI Innovations")
                .count(3)
                .build();

        GenerateTitlesResponse mockResp = GenerateTitlesResponse.builder()
                .titles(List.of("Title 1", "Title 2", "Title 3"))
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.generateTitles(request)).thenReturn(mockResp);
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerateTitlesResponse result = aiService.generateTitles(request, testPrincipal);

        assertThat(result.getTitles()).hasSize(3);
    }

    @Test
    @DisplayName("generateSEO returns metadata and slug")
    void generateSEO_success() {
        GenerateSeoRequest request = GenerateSeoRequest.builder()
                .content("Article content")
                .title("My Title")
                .build();

        GenerateSeoResponse mockResp = GenerateSeoResponse.builder()
                .seoTitle("SEO Title")
                .metaDescription("Meta description")
                .slugSuggestion("seo-title")
                .keywords(List.of("seo", "content"))
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.generateSEO(request)).thenReturn(mockResp);
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerateSeoResponse result = aiService.generateSEO(request, testPrincipal);

        assertThat(result.getSeoTitle()).isEqualTo("SEO Title");
        assertThat(result.getSlugSuggestion()).isEqualTo("seo-title");
    }

    @Test
    @DisplayName("analyze returns full metrics and recommendations")
    void analyze_success() {
        AnalyzeContentRequest request = AnalyzeContentRequest.builder()
                .content("Article content for analysis")
                .title("My Title")
                .build();

        AnalyzeContentResponse mockResp = AnalyzeContentResponse.builder()
                .readabilityScore(85)
                .readabilityGrade("Grade 8")
                .structureScore(90)
                .recommendations(List.of("Add CTA"))
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.analyze(request)).thenReturn(mockResp);
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnalyzeContentResponse result = aiService.analyze(request, testPrincipal);

        assertThat(result.getReadabilityScore()).isEqualTo(85);
        assertThat(result.getRecommendations()).contains("Add CTA");
    }

    // =========================================================================
    // ERROR HANDLING & LOGGING
    // =========================================================================

    @Test
    @DisplayName("provider failure logs FAILED status in ai_responses and throws AIProviderException")
    void providerFailure_logsError_throwsException() {
        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Test Topic")
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.generate(request)).thenThrow(new AIProviderException("Provider timed out"));
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> aiService.generate(request, testPrincipal))
                .isInstanceOf(AIProviderException.class)
                .hasMessageContaining("Provider timed out");

        ArgumentCaptor<AIResponse> respCaptor = ArgumentCaptor.forClass(AIResponse.class);
        verify(aiResponseRepository).save(respCaptor.capture());
        assertThat(respCaptor.getValue().getStatus()).isEqualTo(AIStatus.FAILED);
        assertThat(respCaptor.getValue().getErrorMessage()).contains("Provider timed out");
    }

    @Test
    @DisplayName("malformed AI response logs MALFORMED status and throws MalformedAIResponseException")
    void malformedResponse_logsMalformed_throwsException() {
        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Test Topic")
                .build();

        when(aiProvider.getProviderName()).thenReturn("MOCK");
        when(aiProvider.generate(request)).thenThrow(new MalformedAIResponseException("Invalid JSON"));
        when(aiRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> aiService.generate(request, testPrincipal))
                .isInstanceOf(MalformedAIResponseException.class)
                .hasMessageContaining("Invalid JSON");

        ArgumentCaptor<AIResponse> respCaptor = ArgumentCaptor.forClass(AIResponse.class);
        verify(aiResponseRepository).save(respCaptor.capture());
        assertThat(respCaptor.getValue().getStatus()).isEqualTo(AIStatus.MALFORMED);
    }

    @Test
    @DisplayName("rate limit exceeded blocks request prior to provider call")
    void rateLimitExceeded_blocksExecution() {
        GenerateContentRequest request = GenerateContentRequest.builder()
                .topic("Test Topic")
                .build();

        doThrow(new AIRateLimitExceededException("Limit reached"))
                .when(usageLimitService).checkUsageLimit(testPrincipal);

        assertThatThrownBy(() -> aiService.generate(request, testPrincipal))
                .isInstanceOf(AIRateLimitExceededException.class);

        verify(aiProvider, never()).generate(any());
        verify(aiRequestRepository, never()).save(any());
    }

    // =========================================================================
    // AUDIT HISTORY & USAGE
    // =========================================================================

    @Test
    @DisplayName("getHistory returns paginated audit records")
    void getHistory_success() {
        AIRequest req = AIRequest.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .operation(AIOperation.GENERATE)
                .provider("MOCK")
                .model("gpt-4o-mini")
                .createdAt(Instant.now())
                .build();

        AIResponse resp = AIResponse.builder()
                .id(1L)
                .request(req)
                .status(AIStatus.SUCCESS)
                .totalTokens(120)
                .executionTimeMs(350L)
                .createdAt(Instant.now())
                .build();

        when(aiRequestRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(req)));
        when(aiResponseRepository.findByRequestId(1L)).thenReturn(Optional.of(resp));

        Page<AIHistoryItemResponse> history = aiService.getHistory(null, testPrincipal, PageRequest.of(0, 10));

        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().get(0).getOperation()).isEqualTo(AIOperation.GENERATE);
        assertThat(history.getContent().get(0).getStatus()).isEqualTo(AIStatus.SUCCESS);
    }
}
