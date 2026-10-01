package com.contentmanager.modules.media.service;

import com.contentmanager.common.exception.AppException;
import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.media.dto.MediaResponse;
import com.contentmanager.modules.media.model.Media;
import com.contentmanager.modules.media.repository.MediaRepository;
import com.contentmanager.modules.media.storage.StorageService;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final long maxSizeBytes;
    private final List<String> allowedMimeTypes;

    public MediaService(
            MediaRepository mediaRepository,
            ContentRepository contentRepository,
            UserRepository userRepository,
            StorageService storageService,
            AuditLogService auditLogService,
            @Value("${app.media.max-file-size-bytes:10485760}") long maxSizeBytes,
            @Value("${app.media.allowed-mime-types:image/jpeg,image/png,image/webp,image/gif,image/svg+xml,application/pdf,text/plain,text/markdown}")
            List<String> allowedMimeTypes) {
        this.mediaRepository = mediaRepository;
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedMimeTypes = allowedMimeTypes;
    }

    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, Long contentId, UserPrincipal principal) {
        validateFile(file);

        User uploader = (principal != null && principal.getId() != null)
                ? userRepository.findById(principal.getId()).orElse(null)
                : null;

        Content content = (contentId != null)
                ? contentRepository.findById(contentId).orElse(null)
                : null;

        String cleanOriginalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = getExtension(cleanOriginalName);
        String uniqueFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        storageService.store(file, uniqueFilename);
        String storageUrl = "/api/v1/media/files/" + uniqueFilename;

        Media media = Media.builder()
                .uuid(UUID.randomUUID())
                .filename(uniqueFilename)
                .originalFilename(cleanOriginalName)
                .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .storageUrl(storageUrl)
                .uploadedBy(uploader)
                .content(content)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Media saved = mediaRepository.save(media);

        auditLogService.logAction(
                uploader,
                principal != null ? principal.getEmail() : "anonymous",
                AuditAction.MEDIA_UPLOAD,
                "MEDIA",
                saved.getId().toString(),
                "127.0.0.1",
                "Web",
                String.format("{\"filename\":\"%s\",\"size\":%d}", cleanOriginalName, file.getSize())
        );

        log.info("Media uploaded: id={}, filename={}", saved.getId(), uniqueFilename);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MediaResponse> searchMedia(String keyword, Pageable pageable) {
        return mediaRepository.searchMedia(keyword, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public MediaResponse getMediaById(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", id));
        return mapToResponse(media);
    }

    public Resource loadFileAsResource(String filename) {
        return storageService.loadAsResource(filename);
    }

    @Transactional
    public void deleteMedia(Long id, UserPrincipal principal) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", id));

        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = media.getUploadedBy() != null && media.getUploadedBy().getId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to delete this media file.");
        }

        storageService.delete(media.getFilename());
        mediaRepository.delete(media);

        auditLogService.logAction(
                media.getUploadedBy(),
                principal.getEmail(),
                AuditAction.MEDIA_DELETE,
                "MEDIA",
                id.toString(),
                "127.0.0.1",
                "Web",
                String.format("{\"deletedFilename\":\"%s\"}", media.getOriginalFilename())
        );

        log.info("Media deleted: id={}", id);
    }

    @Transactional
    public MediaResponse associateWithContent(Long mediaId, Long contentId, UserPrincipal principal) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", "id", contentId));

        media.setContent(content);
        Media saved = mediaRepository.save(media);
        return mapToResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("File is empty or not provided.", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxSizeBytes) {
            throw new AppException(String.format("File size (%d bytes) exceeds maximum allowed limit of %d bytes.",
                    file.getSize(), maxSizeBytes), HttpStatus.BAD_REQUEST);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !allowedMimeTypes.contains(mimeType.toLowerCase())) {
            throw new AppException(String.format("File type '%s' is not supported.", mimeType), HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("..")) {
            throw new AppException("Filename contains invalid path sequence.", HttpStatus.BAD_REQUEST);
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0 && idx < filename.length() - 1) ? filename.substring(idx + 1) : "";
    }

    private MediaResponse mapToResponse(Media m) {
        return MediaResponse.builder()
                .id(m.getId())
                .uuid(m.getUuid())
                .filename(m.getFilename())
                .originalFilename(m.getOriginalFilename())
                .mimeType(m.getMimeType())
                .size(m.getSize())
                .storageUrl(m.getStorageUrl())
                .uploadedById(m.getUploadedBy() != null ? m.getUploadedBy().getId() : null)
                .uploadedByName(m.getUploadedBy() != null ? m.getUploadedBy().getName() : "System")
                .contentId(m.getContent() != null ? m.getContent().getId() : null)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
