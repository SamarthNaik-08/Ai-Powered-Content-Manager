package com.contentmanager.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardResponse implements Serializable {

    private ContentCounts counts;
    private List<NameCountItem> byCategory;
    private List<NameCountItem> byType;
    private List<NameCountItem> byAuthor;
    private List<TrendPoint> creationTrend;
    private List<TrendPoint> publishingTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentCounts implements Serializable {
        private long total;
        private long published;
        private long draft;
        private long inReview;
        private long approved;
        private long scheduled;
        private long rejected;
        private long archived;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCountItem implements Serializable {
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint implements Serializable {
        private String date;
        private long count;
    }
}
