package com.contentmanager.modules.media.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {

    private Long id;
    private UUID uuid;
    private String filename;
    private String originalFilename;
    private String mimeType;
    private Long size;
    private String storageUrl;
    private Long uploadedById;
    private String uploadedByName;
    private Long contentId;
    private Instant createdAt;
    private Instant updatedAt;
}
