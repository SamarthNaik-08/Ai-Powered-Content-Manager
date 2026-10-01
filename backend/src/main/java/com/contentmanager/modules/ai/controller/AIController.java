package com.contentmanager.modules.ai.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.ai.dto.*;
import com.contentmanager.modules.ai.service.AIService;
import com.contentmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Intelligence", description = "Endpoints for AI-powered content generation, rewriting, SEO, and analysis")
public class AIController {

    private final AIService aiService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('AI_GENERATE') or hasRole('ADMIN')")
    @Operation(summary = "Generate full content article/post with title, excerpt, and tags")
    public ResponseEntity<ApiResponse<GenerateContentResponse>> generate(
            @Valid @RequestBody GenerateContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        GenerateContentResponse response = aiService.generate(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content generated successfully", response));
    }

    @PostMapping("/rewrite")
    @PreAuthorize("hasAuthority('AI_REWRITE') or hasRole('ADMIN')")
    @Operation(summary = "Rewrite existing content with target tone and style")
    public ResponseEntity<ApiResponse<RewriteContentResponse>> rewrite(
            @Valid @RequestBody RewriteContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RewriteContentResponse response = aiService.rewrite(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content rewritten successfully", response));
    }

    @PostMapping("/summarize")
    @PreAuthorize("hasAuthority('AI_SUMMARIZE') or hasRole('ADMIN')")
    @Operation(summary = "Summarize content and extract key takeaways")
    public ResponseEntity<ApiResponse<SummarizeContentResponse>> summarize(
            @Valid @RequestBody SummarizeContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SummarizeContentResponse response = aiService.summarize(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content summarized successfully", response));
    }

    @PostMapping("/titles")
    @PreAuthorize("hasAuthority('AI_GENERATE') or hasRole('ADMIN')")
    @Operation(summary = "Generate multiple engaging title suggestions")
    public ResponseEntity<ApiResponse<GenerateTitlesResponse>> generateTitles(
            @Valid @RequestBody GenerateTitlesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        GenerateTitlesResponse response = aiService.generateTitles(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Titles generated successfully", response));
    }

    @PostMapping("/seo")
    @PreAuthorize("hasAuthority('AI_SEO') or hasRole('ADMIN')")
    @Operation(summary = "Generate SEO title, meta description, keywords, and slug")
    public ResponseEntity<ApiResponse<GenerateSeoResponse>> generateSEO(
            @Valid @RequestBody GenerateSeoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        GenerateSeoResponse response = aiService.generateSEO(request, principal);
        return ResponseEntity.ok(ApiResponse.success("SEO metadata generated successfully", response));
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAuthority('AI_ANALYZE') or hasRole('ADMIN')")
    @Operation(summary = "Analyze content readability, structure, SEO completeness, and repetition")
    public ResponseEntity<ApiResponse<AnalyzeContentResponse>> analyze(
            @Valid @RequestBody AnalyzeContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AnalyzeContentResponse response = aiService.analyze(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content analysis completed successfully", response));
    }

    @GetMapping("/usage")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's AI daily usage quota and limits")
    public ResponseEntity<ApiResponse<AIUsageResponse>> getUsage(@AuthenticationPrincipal UserPrincipal principal) {
        AIUsageResponse response = aiService.getUsage(principal);
        return ResponseEntity.ok(ApiResponse.success("Usage retrieved successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get audit history of AI requests and responses")
    public ResponseEntity<ApiResponse<Page<AIHistoryItemResponse>>> getHistory(
            @RequestParam(required = false) Long contentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AIHistoryItemResponse> response = aiService.getHistory(contentId, principal, pageable);
        return ResponseEntity.ok(ApiResponse.success("History retrieved successfully", response));
    }
}
