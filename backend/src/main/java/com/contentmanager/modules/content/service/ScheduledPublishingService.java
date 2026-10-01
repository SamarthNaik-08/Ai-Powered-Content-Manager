package com.contentmanager.modules.content.service;

import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentVersion;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.content.repository.ContentVersionRepository;
import com.contentmanager.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPublishingService {

    private final ContentRepository contentRepository;
    private final ContentVersionRepository contentVersionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * Periodically checks for scheduled content whose scheduledAt is past or present,
     * and atomically publishes it to prevent duplicate publishing across instances.
     */
    @Scheduled(fixedDelayString = "${app.scheduling.fixed-delay-ms:30000}")
    @Transactional
    public int publishDueScheduledContent() {
        Instant now = Instant.now();
        List<Content> dueContents = contentRepository.findByStatusAndScheduledAtLessThanEqual(ContentStatus.SCHEDULED, now);

        if (dueContents.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (Content content : dueContents) {
            // Atomic update to prevent duplicate publishing
            int updatedRows = contentRepository.updateStatusAtomically(
                    content.getId(),
                    ContentStatus.SCHEDULED,
                    ContentStatus.PUBLISHED,
                    now,
                    now
            );

            if (updatedRows > 0) {
                publishedCount++;
                log.info("Scheduled content id={} '{}' published successfully at {}", content.getId(), content.getTitle(), now);

                // Record version snapshot
                try {
                    int nextVer = contentVersionRepository.findMaxVersionNumber(content.getId()) + 1;
                    ContentVersion version = ContentVersion.builder()
                            .content(content)
                            .versionNumber(nextVer)
                            .title(content.getTitle())
                            .body(content.getBody())
                            .changeSummary("Published automatically by scheduled publishing service")
                            .createdBy(content.getAuthor())
                            .createdAt(now)
                            .build();
                    contentVersionRepository.save(version);
                } catch (Exception e) {
                    log.warn("Could not save version for scheduled publishing of content id={}: {}", content.getId(), e.getMessage());
                }

                // Audit event
                auditLogService.logSystemAction(
                        AuditAction.PUBLISH_CONTENT,
                        "Content",
                        content.getId().toString(),
                        "Scheduled publication executed for: " + content.getTitle()
                );

                // Notification
                notificationService.notifyScheduledPublished(content);
            } else {
                log.debug("Content id={} was already processed or no longer in SCHEDULED state", content.getId());
            }
        }

        log.info("Scheduled publishing completed. {} content items published.", publishedCount);
        return publishedCount;
    }
}
