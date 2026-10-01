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
public class TagResponse {

    private Long id;
    private String name;
    private String slug;
    private Instant createdAt;
    private Instant updatedAt;
}
