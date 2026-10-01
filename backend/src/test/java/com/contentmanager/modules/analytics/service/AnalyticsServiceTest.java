package com.contentmanager.modules.analytics.service;

import com.contentmanager.modules.ai.model.AIStatus;
import com.contentmanager.modules.ai.repository.AIRequestRepository;
import com.contentmanager.modules.ai.repository.AIResponseRepository;
import com.contentmanager.modules.analytics.dto.AIAnalyticsResponse;
import com.contentmanager.modules.analytics.dto.AnalyticsDashboardResponse;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.repository.ContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock private ContentRepository contentRepository;
    @Mock private AIRequestRepository aiRequestRepository;
    @Mock private AIResponseRepository aiResponseRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Compute dashboard analytics successfully")
    @SuppressWarnings("unchecked")
    void getDashboardAnalytics_success() {
        when(contentRepository.count()).thenReturn(50L);
        when(contentRepository.countByStatus(ContentStatus.PUBLISHED)).thenReturn(25L);
        when(contentRepository.countByStatus(ContentStatus.DRAFT)).thenReturn(15L);
        when(contentRepository.countByStatus(ContentStatus.IN_REVIEW)).thenReturn(5L);
        when(contentRepository.countByStatus(ContentStatus.APPROVED)).thenReturn(2L);
        when(contentRepository.countByStatus(ContentStatus.SCHEDULED)).thenReturn(3L);
        when(contentRepository.countByStatus(ContentStatus.REJECTED)).thenReturn(0L);
        when(contentRepository.countByStatus(ContentStatus.ARCHIVED)).thenReturn(0L);

        when(contentRepository.countGroupedByCategory()).thenReturn(List.<Object[]>of(
                new Object[]{"Technology", 30L},
                new Object[]{"Business", 20L}
        ));

        when(contentRepository.countGroupedByContentType()).thenReturn(List.<Object[]>of(
                new Object[]{"BLOG_POST", 35L},
                new Object[]{"NEWS", 15L}
        ));

        when(contentRepository.countGroupedByAuthor()).thenReturn(List.<Object[]>of(
                new Object[]{"Alice", 25L},
                new Object[]{"Bob", 25L}
        ));

        when(contentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        AnalyticsDashboardResponse response = analyticsService.getDashboardAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getCounts().getTotal()).isEqualTo(50L);
        assertThat(response.getCounts().getPublished()).isEqualTo(25L);
        assertThat(response.getByCategory()).hasSize(2);
        assertThat(response.getByType()).hasSize(2);
        assertThat(response.getByAuthor()).hasSize(2);
        assertThat(response.getCreationTrend()).isNotEmpty();
        assertThat(response.getPublishingTrend()).isNotEmpty();
    }

    @Test
    @DisplayName("Compute AI analytics successfully")
    void getAIAnalytics_success() {
        when(aiRequestRepository.count()).thenReturn(100L);

        when(aiResponseRepository.countGroupedByStatus()).thenReturn(List.<Object[]>of(
                new Object[]{AIStatus.SUCCESS.name(), 95L},
                new Object[]{AIStatus.FAILED.name(), 5L}
        ));

        when(aiResponseRepository.sumTotalTokens()).thenReturn(150000L);
        when(aiResponseRepository.avgExecutionTimeMs()).thenReturn(450.5);

        when(aiRequestRepository.countGroupedByOperation()).thenReturn(List.<Object[]>of(
                new Object[]{"GENERATE", 50L},
                new Object[]{"REWRITE", 30L},
                new Object[]{"SEO", 20L}
        ));

        when(aiRequestRepository.countGroupedByProvider()).thenReturn(List.<Object[]>of(
                new Object[]{"mock", 100L}
        ));

        when(aiRequestRepository.countGroupedByModel()).thenReturn(List.<Object[]>of(
                new Object[]{"gpt-4o-mini", 100L}
        ));

        AIAnalyticsResponse response = analyticsService.getAIAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRequests()).isEqualTo(100L);
        assertThat(response.getSuccessfulRequests()).isEqualTo(95L);
        assertThat(response.getFailedRequests()).isEqualTo(5L);
        assertThat(response.getTotalTokens()).isEqualTo(150000L);
        assertThat(response.getAverageExecutionTimeMs()).isEqualTo(450.5);
        assertThat(response.getByOperation()).containsEntry("GENERATE", 50L);
        assertThat(response.getByProvider()).containsEntry("mock", 100L);
        assertThat(response.getByModel()).containsEntry("gpt-4o-mini", 100L);
    }
}
