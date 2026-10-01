package com.contentmanager.modules.audit.service;

import com.contentmanager.modules.audit.dto.AuditLogResponse;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.model.AuditLog;
import com.contentmanager.modules.audit.repository.AuditLogRepository;
import com.contentmanager.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@example.com").name("User").build();
    }

    @Test
    @DisplayName("Log action persists audit entry")
    void logAction_success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog l = i.getArgument(0);
            l.setId(10L);
            return l;
        });

        AuditLog log = auditLogService.logAction(
                testUser, "user@example.com", AuditAction.CREATE_CONTENT,
                "Content", "100", "127.0.0.1", "Chrome", "Created post"
        );

        assertThat(log).isNotNull();
        assertThat(log.getAction()).isEqualTo(AuditAction.CREATE_CONTENT);
        assertThat(log.getResource()).isEqualTo("Content");
        assertThat(log.getResourceId()).isEqualTo("100");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Sanitizes sensitive tokens, passwords, and secrets from metadata")
    void logAction_sanitizesSensitiveMetadata() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        String rawMetadata = "{\"username\":\"admin\",\"password\":\"secret123\",\"token\":\"jwt.token.val\",\"apiKey\":\"sk-ai-key-99\"}";

        AuditLog log = auditLogService.logAction(
                testUser, "user@example.com", AuditAction.LOGIN,
                "Auth", "1", "127.0.0.1", "Web", rawMetadata
        );

        assertThat(log.getMetadata()).doesNotContain("secret123");
        assertThat(log.getMetadata()).doesNotContain("jwt.token.val");
        assertThat(log.getMetadata()).doesNotContain("sk-ai-key-99");
        assertThat(log.getMetadata()).contains("\"password\":\"***\"");
        assertThat(log.getMetadata()).contains("\"token\":\"***\"");
        assertThat(log.getMetadata()).contains("\"apiKey\":\"***\"");
    }

    @Test
    @DisplayName("Log system action populates system email")
    void logSystemAction_success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logSystemAction(AuditAction.PUBLISH_CONTENT, "Content", "200", "Scheduler trigger");

        verify(auditLogRepository).save(argThat(l ->
                l.getActorEmail().equals("system@contentmanager.com") &&
                l.getAction() == AuditAction.PUBLISH_CONTENT &&
                l.getResourceId().equals("200")
        ));
    }

    @Test
    @DisplayName("Search logs returns paged response")
    @SuppressWarnings("unchecked")
    void searchLogs_success() {
        AuditLog sampleLog = AuditLog.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .actorEmail("test@example.com")
                .action(AuditAction.CREATE_CONTENT)
                .resource("Content")
                .resourceId("10")
                .createdAt(Instant.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog));
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.searchLogs(
                "test@example.com", AuditAction.CREATE_CONTENT, "Content",
                Instant.now().minusSeconds(3600), Instant.now(), PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActorEmail()).isEqualTo("test@example.com");
    }
}
