package com.contentmanager.modules.audit.dto;

import com.contentmanager.modules.audit.model.AuditAction;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private UUID uuid;
    private Long actorId;
    private String actorEmail;
    private AuditAction action;
    private String resource;
    private String resourceId;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    private Instant createdAt;
}
