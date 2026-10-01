package com.contentmanager.modules.user.service;

import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.dto.UpdateUserRoleRequest;
import com.contentmanager.modules.user.dto.UpdateUserStatusRequest;
import com.contentmanager.modules.user.dto.UserResponse;
import com.contentmanager.modules.user.mapper.UserMapper;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.RoleRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementService Tests")
class UserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private User targetUser;
    private User adminUser;
    private UserPrincipal adminPrincipal;
    private Role adminRole;
    private Role editorRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name(RoleName.ROLE_ADMIN).permissions(new HashSet<>()).build();
        editorRole = Role.builder().id(2L).name(RoleName.ROLE_EDITOR).permissions(new HashSet<>()).build();

        adminUser = User.builder()
                .id(1L)
                .email("admin@example.com")
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(UserStatus.ACTIVE)
                .build();

        targetUser = User.builder()
                .id(2L)
                .email("editor@example.com")
                .roles(new HashSet<>(Set.of(editorRole)))
                .status(UserStatus.ACTIVE)
                .build();

        adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@example.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }

    @Test
    @DisplayName("Search users returns paged result")
    @SuppressWarnings("unchecked")
    void searchUsers_success() {
        Page<User> page = new PageImpl<>(List.of(targetUser));
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().id(2L).email("editor@example.com").build());

        Page<UserResponse> result = userService.searchUsers("editor", UserStatus.ACTIVE, "ROLE_EDITOR", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("editor@example.com");
    }

    @Test
    @DisplayName("Update user roles succeeds and records audit log")
    void updateUserRoles_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().id(2L).build());

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder().roles(Set.of("ROLE_ADMIN")).build();
        UserResponse response = userService.updateUserRoles(2L, req, adminPrincipal);

        assertThat(response).isNotNull();
        verify(auditLogService).logPrincipalAction(
                eq(adminPrincipal),
                eq(AuditAction.ROLE_CHANGED),
                eq("User"),
                eq("2"),
                contains("ROLE_ADMIN")
        );
    }

    @Test
    @DisplayName("Admin cannot remove admin role from themselves")
    void updateUserRoles_adminCannotDemoteSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder().roles(Set.of("ROLE_EDITOR")).build();

        assertThatThrownBy(() -> userService.updateUserRoles(1L, req, adminPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot remove the admin role from themselves");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update user status succeeds and records audit log")
    void updateUserStatus_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().id(2L).status(UserStatus.SUSPENDED).build());

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder().status(UserStatus.SUSPENDED).build();
        UserResponse response = userService.updateUserStatus(2L, req, adminPrincipal);

        assertThat(response).isNotNull();
        verify(auditLogService).logPrincipalAction(
                eq(adminPrincipal),
                eq(AuditAction.USER_STATUS_UPDATED),
                eq("User"),
                eq("2"),
                contains("SUSPENDED")
        );
    }

    @Test
    @DisplayName("Admin cannot disable their own account")
    void updateUserStatus_adminCannotDisableSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder().status(UserStatus.INACTIVE).build();

        assertThatThrownBy(() -> userService.updateUserStatus(1L, req, adminPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot disable their own account");

        verify(userRepository, never()).save(any());
    }
}
