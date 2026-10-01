package com.contentmanager.modules.content.controller;

import com.contentmanager.modules.content.dto.CreateContentRequest;
import com.contentmanager.modules.content.dto.ScheduleContentRequest;
import com.contentmanager.modules.content.dto.UpdateContentRequest;
import com.contentmanager.modules.content.model.Content;
import com.contentmanager.modules.content.model.ContentStatus;
import com.contentmanager.modules.content.model.ContentType;
import com.contentmanager.modules.content.repository.ContentRepository;
import com.contentmanager.modules.rbac.PermissionName;
import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.model.Permission;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.PermissionRepository;
import com.contentmanager.modules.user.repository.RoleRepository;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.JwtTokenProvider;
import com.contentmanager.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String authorToken;
    private String otherAuthorToken;
    private User adminUser;
    private User authorUser;
    private User otherAuthorUser;

    @BeforeEach
    void setUp() {
        Permission readPerm = getOrCreatePermission(PermissionName.CONTENT_READ);
        Permission createPerm = getOrCreatePermission(PermissionName.CONTENT_CREATE);
        Permission updatePerm = getOrCreatePermission(PermissionName.CONTENT_UPDATE);
        Permission submitPerm = getOrCreatePermission(PermissionName.CONTENT_SUBMIT);
        Permission approvePerm = getOrCreatePermission(PermissionName.CONTENT_APPROVE);
        Permission publishPerm = getOrCreatePermission(PermissionName.CONTENT_PUBLISH);

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_ADMIN)
                        .description("Admin")
                        .build()));
        adminRole.setPermissions(new HashSet<>(Set.of(readPerm, createPerm, updatePerm, submitPerm, approvePerm, publishPerm)));
        adminRole = roleRepository.save(adminRole);

        Role authorRole = roleRepository.findByName(RoleName.ROLE_AUTHOR).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_AUTHOR)
                        .description("Author")
                        .build()));
        authorRole.setPermissions(new HashSet<>(Set.of(readPerm, createPerm, updatePerm, submitPerm)));
        authorRole = roleRepository.save(authorRole);

        adminUser = userRepository.save(User.builder()
                .email("admin_content_test@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Admin User")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserStatus.ACTIVE)
                .build());

        authorUser = userRepository.save(User.builder()
                .email("author_content_test@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Author User")
                .roles(new HashSet<>(Set.of(authorRole)))
                .status(UserStatus.ACTIVE)
                .build());

        UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(adminPrincipal);

        UserPrincipal authorPrincipal = UserPrincipal.create(authorUser);
        authorToken = jwtTokenProvider.generateAccessToken(authorPrincipal);

        otherAuthorUser = userRepository.save(User.builder()
                .email("other_author_test@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Other Author User")
                .roles(new HashSet<>(Set.of(authorRole)))
                .status(UserStatus.ACTIVE)
                .build());

        UserPrincipal otherAuthorPrincipal = UserPrincipal.create(otherAuthorUser);
        otherAuthorToken = jwtTokenProvider.generateAccessToken(otherAuthorPrincipal);
    }

    private Permission getOrCreatePermission(PermissionName name) {
        return permissionRepository.findByName(name).orElseGet(() ->
                permissionRepository.save(Permission.builder().name(name).description(name.name()).build()));
    }

    @Test
    @DisplayName("Direct API request without token is rejected with 401 Unauthorized")
    void testDirectApiWithoutTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/contents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create content and auto-slug generation via REST endpoint")
    void testCreateContentSuccess() throws Exception {
        CreateContentRequest request = new CreateContentRequest();
        request.setTitle("Enterprise Security Guide 2026");
        request.setBody("Comprehensive guide on production security and hardening.");
        request.setContentType(ContentType.ARTICLE);
        request.setDescription("Security overview");

        mockMvc.perform(post("/api/v1/contents")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Enterprise Security Guide 2026")))
                .andExpect(jsonPath("$.data.slug", containsString("enterprise-security-guide-2026")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")));
    }

    @Test
    @DisplayName("Full workflow transition lifecycle via direct REST API: DRAFT -> IN_REVIEW -> APPROVED -> PUBLISHED")
    void testWorkflowTransitions() throws Exception {
        Content content = contentRepository.save(Content.builder()
                .title("Lifecycle Testing Post")
                .slug("lifecycle-testing-post")
                .body("Initial body content")
                .contentType(ContentType.BLOG_POST)
                .status(ContentStatus.DRAFT)
                .author(authorUser)
                .build());

        // Submit for review (as Author)
        mockMvc.perform(post("/api/v1/contents/" + content.getId() + "/submit")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_REVIEW")));

        // Approve (as Admin)
        mockMvc.perform(post("/api/v1/contents/" + content.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // Publish (as Admin)
        mockMvc.perform(post("/api/v1/contents/" + content.getId() + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.data.publishedAt", notNullValue()));
    }

    @Test
    @DisplayName("Schedule publication via REST API and verify status changes to SCHEDULED")
    void testScheduleContent() throws Exception {
        Content content = contentRepository.save(Content.builder()
                .title("Future Post")
                .slug("future-post")
                .body("Scheduled text")
                .contentType(ContentType.NEWS)
                .status(ContentStatus.APPROVED)
                .author(authorUser)
                .build());

        ScheduleContentRequest request = new ScheduleContentRequest();
        request.setScheduledAt(Instant.now().plus(2, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/contents/" + content.getId() + "/schedule")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.data.scheduledAt", notNullValue()));
    }

    @Test
    @DisplayName("Version restore rolls back body and snapshots new version")
    void testVersionRestore() throws Exception {
        CreateContentRequest request = new CreateContentRequest();
        request.setTitle("Versioned Article");
        request.setBody("Original Version 1 content text");
        request.setContentType(ContentType.ARTICLE);

        String createRes = mockMvc.perform(post("/api/v1/contents")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long contentId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        UpdateContentRequest updateRequest = new UpdateContentRequest();
        updateRequest.setTitle("Versioned Article Updated");
        updateRequest.setBody("Modified Version 2 content text");
        updateRequest.setContentType(ContentType.ARTICLE);

        mockMvc.perform(put("/api/v1/contents/" + contentId)
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/contents/" + contentId + "/versions/1/restore")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body", is("Original Version 1 content text")));
    }

    @Test
    @DisplayName("Unauthorized user cannot modify another author's content (403 Forbidden)")
    void testUnauthorizedUserCannotModifyAnotherAuthorsContent() throws Exception {
        CreateContentRequest request = new CreateContentRequest();
        request.setTitle("Author's Private Content");
        request.setBody("Confidential author draft text");
        request.setContentType(ContentType.ARTICLE);

        String createRes = mockMvc.perform(post("/api/v1/contents")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long contentId = objectMapper.readTree(createRes).path("data").path("id").asLong();

        // Another author attempts to modify
        UpdateContentRequest maliciousUpdate = new UpdateContentRequest();
        maliciousUpdate.setTitle("Illegally Modified Title");

        mockMvc.perform(put("/api/v1/contents/" + contentId)
                        .header("Authorization", "Bearer " + otherAuthorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isForbidden());

        // Another author attempts to delete
        mockMvc.perform(delete("/api/v1/contents/" + contentId)
                        .header("Authorization", "Bearer " + otherAuthorToken))
                .andExpect(status().isForbidden());
    }
}
