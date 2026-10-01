package com.contentmanager.modules.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIUsageResponse {

    private long usedToday;
    private long dailyLimit;
    private long remainingToday;
    private String role;
}
