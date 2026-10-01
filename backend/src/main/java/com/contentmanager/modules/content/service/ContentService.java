package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.*;
import com.contentmanager.modules.content.repository.*;
import com.contentmanager.modules.content.specification.ContentSpecification;
import com.contentmanager.modules.notification.service.NotificationService;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ContentVersionRepository contentVersionRepository;
    private final UserRepository userRepository;
    private final ContentWorkflowService workflowService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public ContentResponse createContent(CreateContentRequest request, UserPrincipal principal) {
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String slug = generateSlug(request.getTitle());

        Content content = Content.builder()
                .title(request.getTitle().trim())
                .slug(slug)
                .description(request.getDescription())
                .body(request.getBody())
                .excerpt(request.getExcerpt())
                .contentType(request.getContentType())
                .status(ContentStatus.DRAFT)
                .author(author)
                .featuredImage(request.getFeaturedImage())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .scheduledAt(request.getScheduledAt())
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            content.setCategory(category);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            content.setTags(tags);
        }

        Content saved = contentRepository.save(content);

        // Create initial version
        createVersion(saved, author, "Initial creation");

        auditLogService.logPrincipalAction(principal, AuditAction.CREATE_CONTENT, "Content", saved.getId().toString(), "Created: " + saved.getTitle());
        log.info("Content created: '{}' by user {}", saved.getTitle(), principal.getEmail());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ContentResponse getContentById(Long id) {
        Content content = contentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
        return mapToResponse(content);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> searchContents(
            String keyword, ContentStatus status, ContentType contentType,
            Long authorId, Long categoryId, Long tagId,
            Instant createdAfter, Instant createdBefore,
            Instant publishedAfter, Instant publishedBefore,
            Pageable pageable) {

        Specification<Content> spec = Specification.where(ContentSpecification.hasKeyword(keyword))
                .and(ContentSpecification.hasStatus(status))
                .and(ContentSpecification.hasContentType(contentType))
                .and(ContentSpecification.hasAuthorId(authorId))
                .and(ContentSpecification.hasCategoryId(categoryId))
                .and(ContentSpecification.hasTagId(tagId))
                .and(ContentSpecification.createdAfter(createdAfter))
                .and(ContentSpecification.createdBefore(createdBefore))
                .and(ContentSpecification.publishedAfter(publishedAfter))
                .and(ContentSpecification.publishedBefore(publishedBefore));

        return contentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public ContentResponse updateContent(Long id, UpdateContentRequest request, UserPrincipal principal) {
        Content content = contentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));

        checkOwnershipOrAdmin(content, principal);

        User editor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean changed = false;

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            content.setTitle(request.getTitle().trim());
            content.setSlug(generateSlug(request.getTitle()));
            changed = true;
        }
        if (request.getDescription() != null) {
            content.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getBody() != null) {
            content.setBody(request.getBody());
            changed = true;
        }
        if (request.getExcerpt() != null) {
            content.setExcerpt(request.getExcerpt());
            changed = true;
        }
        if (request.getContentType() != null) {
            content.setContentType(request.getContentType());
            changed = true;
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            content.setCategory(category);
            changed = true;
        }
        if (request.getFeaturedImage() != null) {
            content.setFeaturedImage(request.getFeaturedImage());
        }
        if (request.getSeoTitle() != null) {
            content.setSeoTitle(request.getSeoTitle());
        }
        if (request.getSeoDescription() != null) {
            content.setSeoDescription(request.getSeoDescription());
        }
        if (request.getScheduledAt() != null) {
            content.setScheduledAt(request.getScheduledAt());
        }
        if (request.getTagIds() != null) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            content.setTags(tags);
            changed = true;
        }

        Content saved = contentRepository.save(content);

        // Create version for meaningful changes
        if (changed) {
            String changeSummary = request.getChangeSummary() != null ? request.getChangeSummary() : "Content updated";
            createVersion(saved, editor, changeSummary);
        }

        auditLogService.logPrincipalAction(principal, AuditAction.UPDATE_CONTENT, "Content", saved.getId().toString(), "Updated: " + saved.getTitle());
        log.info("Content updated: id={} by user {}", id, principal.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteContent(Long id, UserPrincipal principal) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));

        checkOwnershipOrAdmin(content, principal);
        contentRepository.delete(content);
        auditLogService.logPrincipalAction(principal, AuditAction.DELETE_CONTENT, "Content", id.toString(), "Deleted: " + content.getTitle());
        log.info("Content deleted: id={} by user {}", id, principal.getEmail());
    }

    // ========================= WORKFLOW TRANSITIONS =========================

    @Transactional
    public ContentResponse submitForReview(Long id, UserPrincipal principal) {
        ContentResponse res = performTransition(id, ContentStatus.IN_REVIEW, principal, "CONTENT_SUBMIT");
        Content content = getContentEntity(id);
        auditLogService.logPrincipalAction(principal, AuditAction.SUBMIT_CONTENT, "Content", id.toString(), "Submitted for review");
        notificationService.notifyReviewRequested(content);
        return res;
    }

    @Transactional
    public ContentResponse approve(Long id, UserPrincipal principal) {
        ContentResponse res = performTransition(id, ContentStatus.APPROVED, principal, "CONTENT_APPROVE");
        Content content = getContentEntity(id);
        auditLogService.logPrincipalAction(principal, AuditAction.APPROVE_CONTENT, "Content", id.toString(), "Approved content");
        notificationService.notifyContentApproved(content);
        return res;
    }

    @Transactional
    public ContentResponse reject(Long id, UserPrincipal principal) {
        Content content = getContentEntity(id);
        workflowService.validateTransition(content.getStatus(), ContentStatus.REJECTED);
        checkPermission(principal, "CONTENT_APPROVE");

        content.setStatus(ContentStatus.REJECTED);
        Content saved = contentRepository.save(content);
        auditLogService.logPrincipalAction(principal, AuditAction.REJECT_CONTENT, "Content", id.toString(), "Rejected content");
        notificationService.notifyContentRejected(saved);
        log.info("Content rejected: id={} by user {}", id, principal.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public ContentResponse publish(Long id, UserPrincipal principal) {
        Content content = getContentEntity(id);
        workflowService.validateTransition(content.getStatus(), ContentStatus.PUBLISHED);
        checkPermission(principal, "CONTENT_PUBLISH");

        content.setStatus(ContentStatus.PUBLISHED);
        content.setPublishedAt(Instant.now());
        Content saved = contentRepository.save(content);
        auditLogService.logPrincipalAction(principal, AuditAction.PUBLISH_CONTENT, "Content", id.toString(), "Direct publication");
        notificationService.notifyContentPublished(saved);
        log.info("Content published: id={} by user {}", id, principal.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public ContentResponse scheduleContent(Long id, ScheduleContentRequest request, UserPrincipal principal) {
        Content content = getContentEntity(id);
        workflowService.validateTransition(content.getStatus(), ContentStatus.SCHEDULED);
        checkPermission(principal, "CONTENT_PUBLISH");

        if (request.getScheduledAt() == null || request.getScheduledAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }

        content.setStatus(ContentStatus.SCHEDULED);
        content.setScheduledAt(request.getScheduledAt());
        Content saved = contentRepository.save(content);

        auditLogService.logPrincipalAction(
                principal,
                AuditAction.SCHEDULE_CONTENT,
                "Content",
                saved.getId().toString(),
                "Content scheduled for: " + request.getScheduledAt()
        );

        log.info("Content scheduled: id={} at {} by user {}", id, request.getScheduledAt(), principal.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public ContentResponse archive(Long id, UserPrincipal principal) {
        return performTransition(id, ContentStatus.ARCHIVED, principal, "CONTENT_UPDATE");
    }

    // ========================= VERSIONING =========================

    @Transactional(readOnly = true)
    public Page<ContentVersionResponse> getVersions(Long contentId, Pageable pageable) {
        Content content = getContentEntity(contentId);
        return contentVersionRepository.findByContentIdOrderByVersionNumberDesc(content.getId(), pageable)
                .map(this::mapVersionToResponse);
    }

    @Transactional(readOnly = true)
    public ContentVersionResponse getVersion(Long contentId, Integer versionNumber) {
        ContentVersion version = contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for content id: " + contentId));
        return mapVersionToResponse(version);
    }

    @Transactional
    public ContentResponse restoreVersion(Long contentId, Integer versionNumber, UserPrincipal principal) {
        Content content = contentRepository.findByIdWithRelations(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + contentId));

        checkOwnershipOrAdmin(content, principal);

        ContentVersion version = contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for content id: " + contentId));

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        content.setTitle(version.getTitle());
        content.setBody(version.getBody());
        content.setSlug(generateSlug(version.getTitle()));

        Content saved = contentRepository.save(content);
        createVersion(saved, user, "Restored from version " + versionNumber);

        auditLogService.logPrincipalAction(principal, AuditAction.RESTORE_VERSION, "Content", contentId.toString(), "Restored from version " + versionNumber);
        log.info("Content version restored: contentId={}, version={}, by user {}",
                contentId, versionNumber, principal.getEmail());
        return mapToResponse(saved);
    }

    // ========================= DASHBOARD STATS =========================

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalContent(contentRepository.count())
                .drafts(contentRepository.countByStatus(ContentStatus.DRAFT))
                .inReview(contentRepository.countByStatus(ContentStatus.IN_REVIEW))
                .approved(contentRepository.countByStatus(ContentStatus.APPROVED))
                .published(contentRepository.countByStatus(ContentStatus.PUBLISHED))
                .scheduled(contentRepository.countByStatus(ContentStatus.SCHEDULED))
                .archived(contentRepository.countByStatus(ContentStatus.ARCHIVED))
                .rejected(contentRepository.countByStatus(ContentStatus.REJECTED))
                .totalCategories(categoryRepository.count())
                .totalTags(tagRepository.count())
                .build();
    }

    // ========================= HELPERS =========================

    private Content getContentEntity(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
    }

    private ContentResponse performTransition(Long id, ContentStatus targetStatus,
                                               UserPrincipal principal, String requiredPermission) {
        Content content = getContentEntity(id);
        workflowService.validateTransition(content.getStatus(), targetStatus);
        checkPermission(principal, requiredPermission);

        if (targetStatus == ContentStatus.IN_REVIEW) {
            checkOwnershipOrAdmin(content, principal);
        }

        content.setStatus(targetStatus);
        Content saved = contentRepository.save(content);
        log.info("Content transitioned to {}: id={} by user {}", targetStatus, id, principal.getEmail());
        return mapToResponse(saved);
    }

    private void checkOwnershipOrAdmin(Content content, UserPrincipal principal) {
        boolean isOwner = content.getAuthor().getId().equals(principal.getId());
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEditor = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EDITOR"));

        if (!isOwner && !isAdmin && !isEditor) {
            throw new AccessDeniedException("You do not have permission to modify this content.");
        }
    }

    private void checkPermission(UserPrincipal principal, String permission) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean hasPermission = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));

        if (!isAdmin && !hasPermission) {
            throw new AccessDeniedException("You do not have the required permission: " + permission);
        }
    }

    private void createVersion(Content content, User user, String changeSummary) {
        int nextVersion = contentVersionRepository.findMaxVersionNumber(content.getId()) + 1;
        ContentVersion version = ContentVersion.builder()
                .content(content)
                .versionNumber(nextVersion)
                .title(content.getTitle())
                .body(content.getBody())
                .changeSummary(changeSummary)
                .createdBy(user)
                .build();
        contentVersionRepository.save(version);
    }

    private String generateSlug(String title) {
        if (title == null) return "";
        String base = title.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 1;
        while (contentRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private ContentResponse mapToResponse(Content content) {
        ContentResponse.AuthorSummary authorSummary = null;
        if (content.getAuthor() != null) {
            authorSummary = ContentResponse.AuthorSummary.builder()
                    .id(content.getAuthor().getId())
                    .uuid(content.getAuthor().getUuid())
                    .name(content.getAuthor().getName())
                    .email(content.getAuthor().getEmail())
                    .build();
        }

        ContentResponse.CategorySummary categorySummary = null;
        if (content.getCategory() != null) {
            categorySummary = ContentResponse.CategorySummary.builder()
                    .id(content.getCategory().getId())
                    .name(content.getCategory().getName())
                    .slug(content.getCategory().getSlug())
                    .build();
        }

        Set<ContentResponse.TagSummary> tagSummaries = new HashSet<>();
        if (content.getTags() != null) {
            tagSummaries = content.getTags().stream()
                    .map(tag -> ContentResponse.TagSummary.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .slug(tag.getSlug())
                            .build())
                    .collect(Collectors.toSet());
        }

        return ContentResponse.builder()
                .id(content.getId())
                .uuid(content.getUuid())
                .title(content.getTitle())
                .slug(content.getSlug())
                .description(content.getDescription())
                .body(content.getBody())
                .excerpt(content.getExcerpt())
                .contentType(content.getContentType())
                .status(content.getStatus())
                .author(authorSummary)
                .category(categorySummary)
                .tags(tagSummaries)
                .featuredImage(content.getFeaturedImage())
                .seoTitle(content.getSeoTitle())
                .seoDescription(content.getSeoDescription())
                .publishedAt(content.getPublishedAt())
                .scheduledAt(content.getScheduledAt())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .build();
    }

    private ContentVersionResponse mapVersionToResponse(ContentVersion version) {
        return ContentVersionResponse.builder()
                .id(version.getId())
                .contentId(version.getContent().getId())
                .versionNumber(version.getVersionNumber())
                .title(version.getTitle())
                .body(version.getBody())
                .changeSummary(version.getChangeSummary())
                .createdByName(version.getCreatedBy().getName())
                .createdById(version.getCreatedBy().getId())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
