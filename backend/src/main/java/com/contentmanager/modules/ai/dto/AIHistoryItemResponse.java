package com.contentmanager.modules.ai.dto;

import com.contentmanager.modules.ai.model.AIOperation;
import com.contentmanager.modules.ai.model.AIStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIHistoryItemResponse {

    private Long id;
    private UUID uuid;
    private Long contentId;
    private AIOperation operation;
    private String provider;
    private String model;
    private AIStatus status;
    private Integer totalTokens;
    private Long executionTimeMs;
    private String errorMessage;
    private Instant createdAt;
}
