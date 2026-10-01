package com.contentmanager.modules.audit.service;

import com.contentmanager.modules.audit.dto.AuditLogResponse;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.model.AuditLog;
import com.contentmanager.modules.audit.repository.AuditLogRepository;
import com.contentmanager.modules.user.model.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(
            User actor,
            String actorEmail,
            AuditAction action,
            String resource,
            String resourceId,
            String ipAddress,
            String userAgent,
            String metadata) {

        String safeMetadata = sanitizeMetadata(metadata);

        AuditLog logEntry = AuditLog.builder()
                .uuid(UUID.randomUUID())
                .actor(actor)
                .actorEmail(actorEmail != null ? actorEmail : (actor != null ? actor.getEmail() : "system"))
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(safeMetadata)
                .createdAt(Instant.now())
                .build();

        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("Audit log [{}] by {}: {} on {}/{}", action, logEntry.getActorEmail(), resource, resourceId, safeMetadata);
        return saved;
    }

    public void logPrincipalAction(com.contentmanager.security.UserPrincipal principal, AuditAction action, String resource, String resourceId, String metadata) {
        String email = principal != null ? principal.getEmail() : "anonymous";
        logAction(null, email, action, resource, resourceId, "127.0.0.1", "Web Client", metadata);
    }

    public void logUserAction(User user, AuditAction action, String resource, String resourceId, String metadata) {
        String email = user != null ? user.getEmail() : "anonymous";
        logAction(user, email, action, resource, resourceId, "127.0.0.1", "Web Client", metadata);
    }

    public void logSystemAction(AuditAction action, String resource, String resourceId, String metadata) {
        logAction(null, "system@contentmanager.com", action, resource, resourceId, "127.0.0.1", "Internal/System", metadata);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(
            String actorEmail,
            AuditAction action,
            String resource,
            Instant from,
            Instant to,
            Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.trim().toLowerCase() + "%"));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (resource != null && !resource.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("resource")), resource.trim().toLowerCase()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .uuid(log.getUuid())
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorEmail(log.getActorEmail())
                .action(log.getAction())
                .resource(log.getResource())
                .resourceId(log.getResourceId())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String sanitizeMetadata(String metadata) {
        if (metadata == null) return null;
        // Strip any sensitive words or tokens
        return metadata
                .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                .replaceAll("(?i)\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"")
                .replaceAll("(?i)\"apiKey\"\\s*:\\s*\"[^\"]*\"", "\"apiKey\":\"***\"");
    }
}
