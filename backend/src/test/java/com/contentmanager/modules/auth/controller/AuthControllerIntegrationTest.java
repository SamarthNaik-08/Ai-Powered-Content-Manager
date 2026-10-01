package com.contentmanager.modules.auth.controller;

import com.contentmanager.modules.auth.dto.LoginRequest;
import com.contentmanager.modules.auth.dto.RegisterRequest;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private Role adminRole;
    private Role authorRole;
    private Permission contentCreatePermission;

    @BeforeEach
    void setupTestData() {
        contentCreatePermission = permissionRepository.findByName(PermissionName.CONTENT_CREATE)
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .name(PermissionName.CONTENT_CREATE)
                        .description("Create content")
                        .build()));

        adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_ADMIN)
                        .description("Admin")
                        .permissions(new HashSet<>(Set.of(contentCreatePermission)))
                        .build()));

        authorRole = roleRepository.findByName(RoleName.ROLE_AUTHOR)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_AUTHOR)
                        .description("Author")
                        .permissions(new HashSet<>(Set.of(contentCreatePermission)))
                        .build()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should create user and return 201 with token")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("New Author")
                .email("newauthor@example.com")
                .password("Password@1234")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("newauthor@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should fail with 400 when payload is invalid")
    void shouldFailRegistrationOnInvalidPayload() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .name("")
                .email("not-an-email")
                .password("123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should authenticate valid user")
    void shouldLoginSuccessfully() throws Exception {
        User user = User.builder()
                .uuid(UUID.randomUUID())
                .name("Test User")
                .email("testlogin@example.com")
                .passwordHash(passwordEncoder.encode("SecretPass@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(authorRole)))
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("testlogin@example.com")
                .password("SecretPass@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("testlogin@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 on bad credentials")
    void shouldFailLoginOnWrongPassword() throws Exception {
        User user = User.builder()
                .uuid(UUID.randomUUID())
                .name("Test User")
                .email("testwrongpass@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(authorRole)))
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("testwrongpass@example.com")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Protected endpoint should return 401 when no token is provided")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Protected endpoint should return 200 when valid token is provided")
    void shouldAllowAuthenticatedRequest() throws Exception {
        User user = User.builder()
                .uuid(UUID.randomUUID())
                .name("Authorized User")
                .email("authuser@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(authorRole)))
                .build();
        user = userRepository.save(user);

        String token = tokenProvider.generateAccessToken(UserPrincipal.create(user));

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("Hello Authenticated User")));
    }

    @Test
    @DisplayName("Admin-only endpoint should return 403 Forbidden for author user")
    void shouldForbidNonAdminAccess() throws Exception {
        User author = User.builder()
                .uuid(UUID.randomUUID())
                .name("Regular Author")
                .email("regularauthor@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(authorRole)))
                .build();
        author = userRepository.save(author);

        String authorToken = tokenProvider.generateAccessToken(UserPrincipal.create(author));

        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Admin-only endpoint should return 200 for user with ROLE_ADMIN")
    void shouldAllowAdminAccess() throws Exception {
        User admin = User.builder()
                .uuid(UUID.randomUUID())
                .name("Admin User")
                .email("adminuser@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        admin = userRepository.save(admin);

        String adminToken = tokenProvider.generateAccessToken(UserPrincipal.create(admin));

        mockMvc.perform(get("/api/v1/test/admin-only")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("Hello Admin")));
    }

    @Test
    @DisplayName("Permission-based endpoint should allow user possessing CONTENT_CREATE authority")
    void shouldAllowAccessBasedOnPermissionAuthority() throws Exception {
        User author = User.builder()
                .uuid(UUID.randomUUID())
                .name("Creative Author")
                .email("creator@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(authorRole)))
                .build();
        author = userRepository.save(author);

        String token = tokenProvider.generateAccessToken(UserPrincipal.create(author));

        mockMvc.perform(get("/api/v1/test/content-create-permission")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", containsString("Granted access to create content")));
    }
}
