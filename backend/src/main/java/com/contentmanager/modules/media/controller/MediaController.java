package com.contentmanager.modules.media.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.media.dto.MediaResponse;
import com.contentmanager.modules.media.service.MediaService;
import com.contentmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "Endpoints for uploading, managing, and viewing media assets")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = {"", "/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MEDIA_MANAGE') or hasRole('ADMIN')")
    @Operation(summary = "Upload a media file (image, PDF, document)")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contentId", required = false) Long contentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        MediaResponse response = mediaService.uploadMedia(file, contentId, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media uploaded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MEDIA_MANAGE') or hasAuthority('CONTENT_READ') or hasRole('ADMIN')")
    @Operation(summary = "Search and list media files")
    public ResponseEntity<ApiResponse<Page<MediaResponse>>> searchMedia(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<MediaResponse> page = mediaService.searchMedia(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved successfully", page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEDIA_MANAGE') or hasAuthority('CONTENT_READ') or hasRole('ADMIN')")
    @Operation(summary = "Get media details by ID")
    public ResponseEntity<ApiResponse<MediaResponse>> getMediaById(@PathVariable Long id) {
        MediaResponse response = mediaService.getMediaById(id);
        return ResponseEntity.ok(ApiResponse.success("Media retrieved successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MEDIA_MANAGE') or hasRole('ADMIN')")
    @Operation(summary = "Delete a media file")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.deleteMedia(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully", null));
    }

    @PostMapping("/{id}/associate/{contentId}")
    @PreAuthorize("hasAuthority('MEDIA_MANAGE') or hasRole('ADMIN')")
    @Operation(summary = "Associate an existing media file with a content item")
    public ResponseEntity<ApiResponse<MediaResponse>> associateWithContent(
            @PathVariable Long id,
            @PathVariable Long contentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        MediaResponse response = mediaService.associateWithContent(id, contentId, principal);
        return ResponseEntity.ok(ApiResponse.success("Media associated successfully", response));
    }

    @GetMapping("/files/{filename:.+}")
    @Operation(summary = "Serve raw uploaded media file")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = mediaService.loadFileAsResource(filename);
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(file.getURI()));
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}
