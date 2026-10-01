package com.contentmanager.modules.notification.service;

import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.notification.dto.NotificationResponse;
import com.contentmanager.modules.notification.model.Notification;
import com.contentmanager.modules.notification.model.NotificationType;
import com.contentmanager.modules.notification.repository.NotificationRepository;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(
            User user,
            String title,
            String message,
            NotificationType type,
            String link) {

        Notification notification = Notification.builder()
                .uuid(UUID.randomUUID())
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .link(link)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent to {}: '{}' ({})", user.getEmail(), title, type);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UserPrincipal principal, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal principal) {
        return notificationRepository.countByUserIdAndReadFalse(principal.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, UserPrincipal principal) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (!notification.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You cannot mark another user's notification as read.");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Transactional
    public void markAllAsRead(UserPrincipal principal) {
        notificationRepository.markAllAsReadForUser(principal.getId());
    }

    // =========================================================================
    // DOMAIN NOTIFICATION HELPERS
    // =========================================================================

    public void notifyReviewRequested(Content content) {
        // Notify editors/admins
        List<User> users = userRepository.findAll();
        for (User u : users) {
            boolean isEditorOrAdmin = u.getRoles().stream()
                    .map(Role::getName)
                    .anyMatch(r -> r.name().equals("ROLE_ADMIN") || r.name().equals("ROLE_EDITOR"));
            if (isEditorOrAdmin) {
                createNotification(
                        u,
                        "Review Requested: " + content.getTitle(),
                        String.format("'%s' was submitted for review by %s.", content.getTitle(), content.getAuthor().getName()),
                        NotificationType.REVIEW_REQUESTED,
                        "/content/" + content.getId()
                );
            }
        }
    }

    public void notifyContentApproved(Content content) {
        if (content.getAuthor() != null) {
            createNotification(
                    content.getAuthor(),
                    "Content Approved: " + content.getTitle(),
                    String.format("Congratulations! Your content '%s' has been approved for publication.", content.getTitle()),
                    NotificationType.CONTENT_APPROVED,
                    "/content/" + content.getId()
            );
        }
    }

    public void notifyContentRejected(Content content) {
        if (content.getAuthor() != null) {
            createNotification(
                    content.getAuthor(),
                    "Content Review Feedback: " + content.getTitle(),
                    String.format("Your content '%s' was rejected. Please review feedback and revise.", content.getTitle()),
                    NotificationType.CONTENT_REJECTED,
                    "/content/" + content.getId()
            );
        }
    }

    public void notifyContentPublished(Content content) {
        if (content.getAuthor() != null) {
            createNotification(
                    content.getAuthor(),
                    "Content Published: " + content.getTitle(),
                    String.format("Your content '%s' is now live!", content.getTitle()),
                    NotificationType.CONTENT_PUBLISHED,
                    "/content/" + content.getId()
            );
        }
    }

    public void notifyScheduledPublished(Content content) {
        if (content.getAuthor() != null) {
            createNotification(
                    content.getAuthor(),
                    "Scheduled Publication Live: " + content.getTitle(),
                    String.format("Your scheduled content '%s' has been published automatically.", content.getTitle()),
                    NotificationType.SCHEDULED_PUBLISHED,
                    "/content/" + content.getId()
            );
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .uuid(n.getUuid())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .link(n.getLink())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
