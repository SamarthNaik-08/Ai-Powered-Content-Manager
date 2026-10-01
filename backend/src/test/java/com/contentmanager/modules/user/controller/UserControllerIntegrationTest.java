package com.contentmanager.modules.user.controller;

import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.dto.UpdateUserRoleRequest;
import com.contentmanager.modules.user.dto.UpdateUserStatusRequest;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String authorToken;
    private User targetUser;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_ADMIN).description("Admin").build()));
        roleRepository.findByName(RoleName.ROLE_EDITOR).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).description("Editor").build()));
        Role authorRole = roleRepository.findByName(RoleName.ROLE_AUTHOR).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.ROLE_AUTHOR).description("Author").build()));

        User adminUser = userRepository.save(User.builder()
                .email("admin_user_mgmt@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Super Admin")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserStatus.ACTIVE)
                .build());

        User authorUser = userRepository.save(User.builder()
                .email("author_user_mgmt@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Regular Author")
                .roles(new HashSet<>(Set.of(authorRole)))
                .status(UserStatus.ACTIVE)
                .build());

        targetUser = userRepository.save(User.builder()
                .email("target_mgmt@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Target User")
                .roles(new HashSet<>(Set.of(authorRole)))
                .status(UserStatus.ACTIVE)
                .build());

        UserPrincipal adminPrincipal = UserPrincipal.create(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(adminPrincipal);

        UserPrincipal authorPrincipal = UserPrincipal.create(authorUser);
        authorToken = jwtTokenProvider.generateAccessToken(authorPrincipal);
    }

    @Test
    @DisplayName("Non-admin user cannot access user list - returns 403 Forbidden")
    void testNonAdminCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can list and search users")
    void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Admin can update user status to SUSPENDED")
    void testAdminCanSuspendUser() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.SUSPENDED);

        mockMvc.perform(put("/api/v1/users/" + targetUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")));
    }

    @Test
    @DisplayName("Admin can update user roles to ROLE_EDITOR")
    void testAdminCanUpdateRoles() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRoles(Set.of("ROLE_EDITOR"));

        mockMvc.perform(put("/api/v1/users/" + targetUser.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]", is("ROLE_EDITOR")));
    }
}
