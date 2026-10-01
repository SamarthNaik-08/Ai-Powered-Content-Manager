package com.contentmanager.modules.audit.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.audit.dto.AuditLogResponse;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Enterprise security and operational audit trail APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW') or hasRole('ADMIN')")
    @Operation(summary = "Search and filter platform audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AuditLogResponse> logs = auditLogService.searchLogs(actorEmail, action, resource, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }
}
