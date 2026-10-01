package com.contentmanager.modules.analytics.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.analytics.dto.AIAnalyticsResponse;
import com.contentmanager.modules.analytics.dto.AnalyticsDashboardResponse;
import com.contentmanager.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Insights", description = "Enterprise platform metrics, content publishing velocity, and AI token telemetry")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get content velocity, status distribution, and category metrics (Cached)")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<AnalyticsDashboardResponse>> getDashboardAnalytics() {
        AnalyticsDashboardResponse response = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get AI token usage, request velocity, and model latency telemetry (Cached)")
    @GetMapping("/ai")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<ApiResponse<AIAnalyticsResponse>> getAIAnalytics() {
        AIAnalyticsResponse response = analyticsService.getAIAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
