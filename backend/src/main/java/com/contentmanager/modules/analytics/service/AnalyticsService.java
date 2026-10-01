package com.contentmanager.modules.analytics.service;

import com.contentmanager.modules.ai.model.AIStatus;
import com.contentmanager.modules.ai.repository.AIRequestRepository;
import com.contentmanager.modules.ai.repository.AIResponseRepository;
import com.contentmanager.modules.analytics.dto.AIAnalyticsResponse;
import com.contentmanager.modules.analytics.dto.AnalyticsDashboardResponse;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.content.specification.ContentSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ContentRepository contentRepository;
    private final AIRequestRepository aiRequestRepository;
    private final AIResponseRepository aiResponseRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics-dashboard", unless = "#result == null")
    public AnalyticsDashboardResponse getDashboardAnalytics() {
        log.info("Computing dashboard analytics (cached if Redis/Cache is active)");

        // 1. Content Counts
        AnalyticsDashboardResponse.ContentCounts counts = AnalyticsDashboardResponse.ContentCounts.builder()
                .total(contentRepository.count())
                .published(contentRepository.countByStatus(ContentStatus.PUBLISHED))
                .draft(contentRepository.countByStatus(ContentStatus.DRAFT))
                .inReview(contentRepository.countByStatus(ContentStatus.IN_REVIEW))
                .approved(contentRepository.countByStatus(ContentStatus.APPROVED))
                .scheduled(contentRepository.countByStatus(ContentStatus.SCHEDULED))
                .rejected(contentRepository.countByStatus(ContentStatus.REJECTED))
                .archived(contentRepository.countByStatus(ContentStatus.ARCHIVED))
                .build();

        // 2. By Category
        List<AnalyticsDashboardResponse.NameCountItem> byCategory = contentRepository.countGroupedByCategory().stream()
                .map(row -> new AnalyticsDashboardResponse.NameCountItem(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // 3. By Type
        List<AnalyticsDashboardResponse.NameCountItem> byType = contentRepository.countGroupedByContentType().stream()
                .map(row -> new AnalyticsDashboardResponse.NameCountItem(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // 4. By Author
        List<AnalyticsDashboardResponse.NameCountItem> byAuthor = contentRepository.countGroupedByAuthor().stream()
                .map(row -> new AnalyticsDashboardResponse.NameCountItem(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // 5. Creation & Publishing Trends (Past 14 Days)
        Instant fourteenDaysAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        Specification<Content> recentSpec = Specification.where(ContentSpecification.createdAfter(fourteenDaysAgo));
        List<Content> recentContents = contentRepository.findAll(recentSpec, PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "createdAt"))).getContent();

        Map<String, Long> creationByDate = new TreeMap<>();
        Map<String, Long> publishingByDate = new TreeMap<>();

        // Prepopulate past 14 days with 0 for smooth chart lines
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 13; i >= 0; i--) {
            String dateKey = today.minusDays(i).toString();
            creationByDate.put(dateKey, 0L);
            publishingByDate.put(dateKey, 0L);
        }

        for (Content c : recentContents) {
            if (c.getCreatedAt() != null) {
                String d = c.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
                creationByDate.computeIfPresent(d, (k, v) -> v + 1);
            }
            if (c.getPublishedAt() != null) {
                String d = c.getPublishedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
                publishingByDate.computeIfPresent(d, (k, v) -> v + 1);
            }
        }

        List<AnalyticsDashboardResponse.TrendPoint> creationTrend = creationByDate.entrySet().stream()
                .map(e -> new AnalyticsDashboardResponse.TrendPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        List<AnalyticsDashboardResponse.TrendPoint> publishingTrend = publishingByDate.entrySet().stream()
                .map(e -> new AnalyticsDashboardResponse.TrendPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return AnalyticsDashboardResponse.builder()
                .counts(counts)
                .byCategory(byCategory)
                .byType(byType)
                .byAuthor(byAuthor)
                .creationTrend(creationTrend)
                .publishingTrend(publishingTrend)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics-ai", unless = "#result == null")
    public AIAnalyticsResponse getAIAnalytics() {
        log.info("Computing AI analytics (cached if Redis/Cache is active)");

        long totalRequests = aiRequestRepository.count();

        Map<String, Long> byStatus = aiResponseRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        long successfulRequests = byStatus.getOrDefault(AIStatus.SUCCESS.name(), 0L);
        long failedRequests = byStatus.getOrDefault(AIStatus.FAILED.name(), 0L)
                + byStatus.getOrDefault(AIStatus.MALFORMED.name(), 0L);

        long totalTokens = aiResponseRepository.sumTotalTokens();
        double avgTimeMs = aiResponseRepository.avgExecutionTimeMs();

        Map<String, Long> byOperation = aiRequestRepository.countGroupedByOperation().stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        Map<String, Long> byProvider = aiRequestRepository.countGroupedByProvider().stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        Map<String, Long> byModel = aiRequestRepository.countGroupedByModel().stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        return AIAnalyticsResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .totalTokens(totalTokens)
                .averageExecutionTimeMs(Math.round(avgTimeMs * 100.0) / 100.0)
                .byOperation(byOperation)
                .byProvider(byProvider)
                .byModel(byModel)
                .build();
    }
}
