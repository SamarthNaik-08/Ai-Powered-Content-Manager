package com.contentmanager.modules.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentVersionResponse {

    private Long id;
    private Long contentId;
    private Integer versionNumber;
    private String title;
    private String body;
    private String changeSummary;
    private String createdByName;
    private Long createdById;
    private Instant createdAt;
}
