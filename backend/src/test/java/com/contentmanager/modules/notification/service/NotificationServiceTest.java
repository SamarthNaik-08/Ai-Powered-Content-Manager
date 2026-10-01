package com.contentmanager.modules.notification.service;

import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.notification.dto.NotificationResponse;
import com.contentmanager.modules.notification.model.Notification;
import com.contentmanager.modules.notification.model.NotificationType;
import com.contentmanager.modules.notification.repository.NotificationRepository;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user1;
    private UserPrincipal principal1;
    private UserPrincipal principal2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).email("user1@example.com").name("User 1").build();
        principal1 = UserPrincipal.builder().id(1L).email("user1@example.com").build();
        principal2 = UserPrincipal.builder().id(2L).email("user2@example.com").build();
    }

    @Test
    @DisplayName("Create notification successfully")
    void createNotification_success() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(10L);
            return n;
        });

        Notification result = notificationService.createNotification(
                user1, "Title", "Message", NotificationType.CONTENT_APPROVED, "/content/1"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Title");
        assertThat(result.getType()).isEqualTo(NotificationType.CONTENT_APPROVED);
        assertThat(result.isRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Get unread notifications count")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(principal1);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByUserIdAndReadFalse(1L);
    }

    @Test
    @DisplayName("Mark notification as read by owner succeeds")
    void markAsRead_byOwner_success() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(user1)
                .title("Test")
                .read(false)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(10L, principal1);

        assertThat(response.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Mark notification as read by another user throws AccessDeniedException")
    void markAsRead_byAnotherUser_throwsAccessDenied() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(user1)
                .title("Test")
                .read(false)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(10L, principal2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You cannot mark another user's notification as read");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mark all as read invokes repository batch update")
    void markAllAsRead_success() {
        notificationService.markAllAsRead(principal1);
        verify(notificationRepository).markAllAsReadForUser(1L);
    }

    @Test
    @DisplayName("Notify content approved dispatches notification to author")
    void notifyContentApproved_success() {
        Content content = Content.builder()
                .id(50L)
                .title("Awesome Blog")
                .author(user1)
                .build();

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.notifyContentApproved(content);

        verify(notificationRepository).save(argThat(n ->
                n.getUser().equals(user1) &&
                n.getType() == NotificationType.CONTENT_APPROVED &&
                n.getTitle().contains("Awesome Blog")
        ));
    }
}
