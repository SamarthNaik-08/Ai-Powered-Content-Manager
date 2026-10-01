package com.contentmanager.modules.content.service;

import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.content.repository.ContentVersionRepository;
import com.contentmanager.modules.notification.service.NotificationService;
import com.contentmanager.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledPublishingService Tests")
class ScheduledPublishingServiceTest {

    @Mock private ContentRepository contentRepository;
    @Mock private ContentVersionRepository contentVersionRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ScheduledPublishingService scheduledPublishingService;

    private Content sampleScheduledContent;
    private User author;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).email("author@example.com").name("Author").build();
        sampleScheduledContent = Content.builder()
                .id(100L)
                .title("Scheduled Article")
                .body("Content body")
                .status(ContentStatus.SCHEDULED)
                .author(author)
                .scheduledAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();
    }

    @Test
    @DisplayName("Publishes due scheduled content and triggers audit and notification")
    void publishDueScheduledContent_success() {
        when(contentRepository.findByStatusAndScheduledAtLessThanEqual(eq(ContentStatus.SCHEDULED), any(Instant.class)))
                .thenReturn(List.of(sampleScheduledContent));

        when(contentRepository.updateStatusAtomically(
                eq(100L),
                eq(ContentStatus.SCHEDULED),
                eq(ContentStatus.PUBLISHED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(1);

        when(contentVersionRepository.findMaxVersionNumber(100L)).thenReturn(1);

        int published = scheduledPublishingService.publishDueScheduledContent();

        assertThat(published).isEqualTo(1);
        verify(contentVersionRepository).save(any());
        verify(auditLogService).logSystemAction(
                eq(AuditAction.PUBLISH_CONTENT),
                eq("Content"),
                eq("100"),
                contains("Scheduled publication executed")
        );
        verify(notificationService).notifyScheduledPublished(sampleScheduledContent);
    }

    @Test
    @DisplayName("Prevents duplicate publishing if atomic update returns 0")
    void publishDueScheduledContent_preventsDuplicatePublishing() {
        when(contentRepository.findByStatusAndScheduledAtLessThanEqual(eq(ContentStatus.SCHEDULED), any(Instant.class)))
                .thenReturn(List.of(sampleScheduledContent));

        // Atomic update returns 0 -> another scheduler already processed it
        when(contentRepository.updateStatusAtomically(
                eq(100L),
                eq(ContentStatus.SCHEDULED),
                eq(ContentStatus.PUBLISHED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(0);

        int published = scheduledPublishingService.publishDueScheduledContent();

        assertThat(published).isEqualTo(0);
        verifyNoInteractions(contentVersionRepository);
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Returns 0 when no contents are due for scheduling")
    void publishDueScheduledContent_noneDue() {
        when(contentRepository.findByStatusAndScheduledAtLessThanEqual(eq(ContentStatus.SCHEDULED), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        int published = scheduledPublishingService.publishDueScheduledContent();

        assertThat(published).isEqualTo(0);
        verify(contentRepository, never()).updateStatusAtomically(any(), any(), any(), any(), any());
    }
}
