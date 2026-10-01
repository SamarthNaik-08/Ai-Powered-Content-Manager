package com.contentmanager.modules.content.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentType;
import com.contentmanager.modules.content.service.ContentService;
import com.contentmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "Content Management", description = "Core CMS CRUD, workflow transitions, scheduling, and versioning")
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "Search and filter content with pagination")
    @GetMapping
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<Page<ContentResponse>>> searchContents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContentResponse> result = contentService.searchContents(
                keyword, status, contentType, authorId, categoryId, tagId,
                createdAfter, createdBefore, publishedAfter, publishedBefore, pageable);

        return ResponseEntity.ok(ApiResponse.success("Contents retrieved successfully", result));
    }

    @Operation(summary = "Get content item by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<ContentResponse>> getContentById(@PathVariable Long id) {
        ContentResponse response = contentService.getContentById(id);
        return ResponseEntity.ok(ApiResponse.success("Content retrieved successfully", response));
    }

    @Operation(summary = "Create draft content item")
    @PostMapping
    @PreAuthorize("hasAuthority('CONTENT_CREATE')")
    public ResponseEntity<ApiResponse<ContentResponse>> createContent(
            @Valid @RequestBody CreateContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.createContent(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Content created successfully", response));
    }

    @Operation(summary = "Update content item and snapshot version")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTENT_UPDATE')")
    public ResponseEntity<ApiResponse<ContentResponse>> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.updateContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content updated successfully", response));
    }

    @Operation(summary = "Delete content item")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTENT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteContent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        contentService.deleteContent(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content deleted successfully", null));
    }

    // ========================= WORKFLOW ENDPOINTS =========================

    @Operation(summary = "Submit draft content for editorial review")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('CONTENT_SUBMIT')")
    public ResponseEntity<ApiResponse<ContentResponse>> submitForReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.submitForReview(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content submitted for review", response));
    }

    @Operation(summary = "Approve reviewed content for publishing")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('CONTENT_APPROVE')")
    public ResponseEntity<ApiResponse<ContentResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.approve(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content approved", response));
    }

    @Operation(summary = "Reject reviewed content with feedback")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('CONTENT_APPROVE')")
    public ResponseEntity<ApiResponse<ContentResponse>> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.reject(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content rejected", response));
    }

    @Operation(summary = "Publish approved content immediately")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('CONTENT_PUBLISH')")
    public ResponseEntity<ApiResponse<ContentResponse>> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.publish(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content published", response));
    }

    @Operation(summary = "Schedule approved content for automatic future publication")
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAuthority('CONTENT_PUBLISH')")
    public ResponseEntity<ApiResponse<ContentResponse>> schedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleContentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.scheduleContent(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Content scheduled successfully", response));
    }

    @Operation(summary = "Archive content item")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('CONTENT_UPDATE')")
    public ResponseEntity<ApiResponse<ContentResponse>> archive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.archive(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Content archived", response));
    }

    // ========================= VERSIONING ENDPOINTS =========================

    @Operation(summary = "Get historical version snapshots for content")
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<Page<ContentVersionResponse>>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentVersionResponse> result = contentService.getVersions(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Versions retrieved successfully", result));
    }

    @Operation(summary = "Get specific version snapshot content")
    @GetMapping("/{id}/versions/{versionNumber}")
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<ContentVersionResponse>> getVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber) {
        ContentVersionResponse response = contentService.getVersion(id, versionNumber);
        return ResponseEntity.ok(ApiResponse.success("Version retrieved successfully", response));
    }

    @Operation(summary = "Restore content state to a historical version snapshot")
    @PostMapping("/{id}/versions/{versionNumber}/restore")
    @PreAuthorize("hasAuthority('CONTENT_UPDATE')")
    public ResponseEntity<ApiResponse<ContentResponse>> restoreVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContentResponse response = contentService.restoreVersion(id, versionNumber, principal);
        return ResponseEntity.ok(ApiResponse.success("Version restored successfully", response));
    }

    // ========================= DASHBOARD STATS =========================

    @Operation(summary = "Get quick dashboard metrics for content counts and states")
    @GetMapping("/stats/dashboard")
    @PreAuthorize("hasAuthority('CONTENT_READ')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = contentService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }
}
