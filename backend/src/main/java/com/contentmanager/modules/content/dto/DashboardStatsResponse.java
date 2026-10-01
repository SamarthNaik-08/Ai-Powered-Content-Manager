package com.contentmanager.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalContent;
    private long drafts;
    private long inReview;
    private long approved;
    private long published;
    private long scheduled;
    private long archived;
    private long rejected;
    private long totalCategories;
    private long totalTags;
}
