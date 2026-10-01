package com.contentmanager.modules.notification.dto;

import com.contentmanager.modules.notification.model.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private UUID uuid;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private String link;
    private Instant createdAt;
}
