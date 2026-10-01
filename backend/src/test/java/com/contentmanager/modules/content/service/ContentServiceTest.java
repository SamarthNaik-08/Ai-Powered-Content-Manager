package com.contentmanager.modules.content.service;

import com.contentmanager.common.exception.ResourceNotFoundException;
import com.contentmanager.modules.content.dto.*;
import com.contentmanager.modules.content.model.*;
import com.contentmanager.modules.content.repository.*;
import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService Tests")
class ContentServiceTest {

    @Mock private ContentRepository contentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private ContentVersionRepository contentVersionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentWorkflowService workflowService;
    @Mock private com.contentmanager.modules.audit.service.AuditLogService auditLogService;
    @Mock private com.contentmanager.modules.notification.service.NotificationService notificationService;

    @InjectMocks private ContentService contentService;

    private User testUser;
    private UserPrincipal testPrincipal;
    private Content testContent;
    private Category testCategory;
    private Tag testTag;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder()
                .id(1L)
                .name(RoleName.ROLE_ADMIN)
                .permissions(new HashSet<>())
                .build();

        testUser = User.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .name("Test Author")
                .email("author@test.com")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testPrincipal = UserPrincipal.create(testUser);

        testCategory = Category.builder()
                .id(1L)
                .name("Technology")
                .slug("technology")
                .description("Tech articles")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testTag = Tag.builder()
                .id(1L)
                .name("Featured")
                .slug("featured")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testContent = Content.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .title("Test Article")
                .slug("test-article")
                .body("This is a test article body.")
                .excerpt("Test excerpt")
                .contentType(ContentType.ARTICLE)
                .status(ContentStatus.DRAFT)
                .author(testUser)
                .category(testCategory)
                .tags(new HashSet<>(Set.of(testTag)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ========================= CREATE =========================

    @Test
    @DisplayName("createContent creates a new draft content")
    void createContent_success() {
        CreateContentRequest request = CreateContentRequest.builder()
                .title("New Article")
                .contentType(ContentType.ARTICLE)
                .body("Body text")
                .categoryId(1L)
                .tagIds(Set.of(1L))
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(contentRepository.existsBySlug(anyString())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(tagRepository.findByIdIn(anySet())).thenReturn(Set.of(testTag));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> {
            Content c = inv.getArgument(0);
            c.setId(2L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        when(contentVersionRepository.findMaxVersionNumber(anyLong())).thenReturn(0);
        when(contentVersionRepository.save(any())).thenReturn(ContentVersion.builder().build());

        ContentResponse response = contentService.createContent(request, testPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("New Article");
        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        verify(contentRepository).save(any(Content.class));
        verify(contentVersionRepository).save(any(ContentVersion.class));
    }

    // ========================= GET =========================

    @Test
    @DisplayName("getContentById returns content")
    void getContentById_success() {
        when(contentRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(testContent));

        ContentResponse response = contentService.getContentById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("getContentById throws for non-existent id")
    void getContentById_notFound() {
        when(contentRepository.findByIdWithRelations(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getContentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ========================= SEARCH =========================

    @Test
    @DisplayName("searchContents with filters returns paginated results")
    @SuppressWarnings("unchecked")
    void searchContents_withFilters() {
        Page<Content> page = new PageImpl<>(List.of(testContent), PageRequest.of(0, 20), 1);
        when(contentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ContentResponse> result = contentService.searchContents(
                "test", ContentStatus.DRAFT, ContentType.ARTICLE,
                null, null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Article");
    }

    // ========================= DELETE =========================

    @Test
    @DisplayName("deleteContent removes content for owner")
    void deleteContent_success() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));

        contentService.deleteContent(1L, testPrincipal);

        verify(contentRepository).delete(testContent);
    }

    // ========================= WORKFLOW: SUBMIT =========================

    @Test
    @DisplayName("submitForReview transitions DRAFT to IN_REVIEW")
    void submitForReview_success() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        doNothing().when(workflowService).validateTransition(ContentStatus.DRAFT, ContentStatus.IN_REVIEW);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentResponse response = contentService.submitForReview(1L, testPrincipal);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.IN_REVIEW);
    }

    // ========================= WORKFLOW: PUBLISH =========================

    @Test
    @DisplayName("publish transitions APPROVED to PUBLISHED with publishedAt")
    void publish_success() {
        testContent.setStatus(ContentStatus.APPROVED);
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        doNothing().when(workflowService).validateTransition(ContentStatus.APPROVED, ContentStatus.PUBLISHED);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentResponse response = contentService.publish(1L, testPrincipal);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    // ========================= WORKFLOW: INVALID TRANSITION =========================

    @Test
    @DisplayName("publish from DRAFT throws IllegalStateException")
    void publish_fromDraft_throws() {
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        doThrow(new IllegalStateException("Cannot transition from DRAFT to PUBLISHED"))
                .when(workflowService).validateTransition(ContentStatus.DRAFT, ContentStatus.PUBLISHED);

        assertThatThrownBy(() -> contentService.publish(1L, testPrincipal))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    // ========================= VERSIONING =========================

    @Test
    @DisplayName("getVersions returns paginated version history")
    void getVersions_success() {
        ContentVersion version = ContentVersion.builder()
                .id(1L)
                .content(testContent)
                .versionNumber(1)
                .title("Test Article")
                .body("Body")
                .changeSummary("Initial creation")
                .createdBy(testUser)
                .createdAt(Instant.now())
                .build();

        Page<ContentVersion> page = new PageImpl<>(List.of(version), PageRequest.of(0, 20), 1);
        when(contentRepository.findById(1L)).thenReturn(Optional.of(testContent));
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<ContentVersionResponse> result = contentService.getVersions(1L, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("restoreVersion restores content from a specific version")
    void restoreVersion_success() {
        ContentVersion version = ContentVersion.builder()
                .id(1L)
                .content(testContent)
                .versionNumber(1)
                .title("Old Title")
                .body("Old Body")
                .changeSummary("Initial creation")
                .createdBy(testUser)
                .createdAt(Instant.now())
                .build();

        when(contentRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(testContent));
        when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(version));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(contentRepository.existsBySlug(anyString())).thenReturn(false);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentVersionRepository.findMaxVersionNumber(anyLong())).thenReturn(1);
        when(contentVersionRepository.save(any())).thenReturn(ContentVersion.builder().build());

        ContentResponse response = contentService.restoreVersion(1L, 1, testPrincipal);

        assertThat(response.getTitle()).isEqualTo("Old Title");
        assertThat(response.getBody()).isEqualTo("Old Body");
        verify(contentVersionRepository, times(1)).save(any(ContentVersion.class));
    }

    // ========================= DASHBOARD STATS =========================

    @Test
    @DisplayName("getDashboardStats returns all counts")
    void getDashboardStats_success() {
        when(contentRepository.count()).thenReturn(10L);
        when(contentRepository.countByStatus(ContentStatus.DRAFT)).thenReturn(3L);
        when(contentRepository.countByStatus(ContentStatus.IN_REVIEW)).thenReturn(2L);
        when(contentRepository.countByStatus(ContentStatus.APPROVED)).thenReturn(1L);
        when(contentRepository.countByStatus(ContentStatus.PUBLISHED)).thenReturn(2L);
        when(contentRepository.countByStatus(ContentStatus.SCHEDULED)).thenReturn(1L);
        when(contentRepository.countByStatus(ContentStatus.ARCHIVED)).thenReturn(1L);
        when(contentRepository.countByStatus(ContentStatus.REJECTED)).thenReturn(0L);
        when(categoryRepository.count()).thenReturn(5L);
        when(tagRepository.count()).thenReturn(8L);

        DashboardStatsResponse stats = contentService.getDashboardStats();

        assertThat(stats.getTotalContent()).isEqualTo(10);
        assertThat(stats.getDrafts()).isEqualTo(3);
        assertThat(stats.getInReview()).isEqualTo(2);
        assertThat(stats.getPublished()).isEqualTo(2);
        assertThat(stats.getTotalCategories()).isEqualTo(5);
        assertThat(stats.getTotalTags()).isEqualTo(8);
    }
}
