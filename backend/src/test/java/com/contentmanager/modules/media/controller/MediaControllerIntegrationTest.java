package com.contentmanager.modules.media.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authorToken;

    @BeforeEach
    void setUp() {
        Permission readPerm = permissionRepository.findByName(PermissionName.CONTENT_READ).orElseGet(() ->
                permissionRepository.save(Permission.builder().name(PermissionName.CONTENT_READ).description("Read content").build()));
        Permission createPerm = permissionRepository.findByName(PermissionName.CONTENT_CREATE).orElseGet(() ->
                permissionRepository.save(Permission.builder().name(PermissionName.CONTENT_CREATE).description("Create content").build()));
        Permission mediaManagePerm = permissionRepository.findByName(PermissionName.MEDIA_MANAGE).orElseGet(() ->
                permissionRepository.save(Permission.builder().name(PermissionName.MEDIA_MANAGE).description("Manage media").build()));

        Role authorRole = roleRepository.findByName(RoleName.ROLE_AUTHOR).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_AUTHOR)
                        .description("Author")
                        .build()));
        authorRole.setPermissions(new HashSet<>(Set.of(readPerm, createPerm, mediaManagePerm)));
        authorRole = roleRepository.save(authorRole);

        User authorUser = userRepository.save(User.builder()
                .email("author_media_test@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Media Author")
                .roles(new HashSet<>(Set.of(authorRole)))
                .status(UserStatus.ACTIVE)
                .build());

        UserPrincipal authorPrincipal = UserPrincipal.create(authorUser);
        authorToken = jwtTokenProvider.generateAccessToken(authorPrincipal);
    }

    @Test
    @DisplayName("Upload valid image successfully via multipart endpoint")
    void testUploadValidMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake image byte content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename", is("sample-image.png")))
                .andExpect(jsonPath("$.data.filename").isNotEmpty())
                .andExpect(jsonPath("$.data.mimeType", is("image/png")));
    }

    @Test
    @DisplayName("Upload unsupported file MIME type is rejected with 400 Bad Request")
    void testUploadInvalidMimeType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "binary executable code".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("List media files with pagination")
    void testListMedia() throws Exception {
        mockMvc.perform(get("/api/v1/media")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
