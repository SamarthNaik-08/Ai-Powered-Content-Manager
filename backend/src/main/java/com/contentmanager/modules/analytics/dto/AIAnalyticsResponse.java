package com.contentmanager.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalyticsResponse implements Serializable {

    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalTokens;
    private double averageExecutionTimeMs;
    private Map<String, Long> byOperation;
    private Map<String, Long> byProvider;
    private Map<String, Long> byModel;
}
