package com.contentmanager.modules.user.service;

import com.contentmanager.common.exception.ResourceNotFoundException;
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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user session found");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUuid(UUID uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with uuid: " + uuid));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, UserStatus status, String roleName, Pageable pageable) {
        Specification<User> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), searchPattern);
                predicates.add(cb.or(nameLike, emailLike));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (roleName != null && !roleName.isBlank()) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                try {
                    RoleName rName = RoleName.valueOf(roleName.trim());
                    predicates.add(cb.equal(roleJoin.get("name"), rName));
                } catch (IllegalArgumentException ignored) {
                }
            }

            q.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, UpdateUserRoleRequest request, UserPrincipal adminPrincipal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Prevent admin from removing admin role from themselves
        if (user.getId().equals(adminPrincipal.getId()) &&
                !request.getRoles().contains("ROLE_ADMIN") &&
                adminPrincipal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Administrators cannot remove the admin role from themselves.");
        }

        Set<Role> newRoles = new HashSet<>();
        for (String rName : request.getRoles()) {
            RoleName roleEnum;
            try {
                roleEnum = RoleName.valueOf(rName.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role name: " + rName);
            }
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleEnum));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        User saved = userRepository.save(user);

        auditLogService.logPrincipalAction(
                adminPrincipal,
                AuditAction.ROLE_CHANGED,
                "User",
                user.getId().toString(),
                "Roles updated to: " + request.getRoles()
        );

        log.info("Roles updated for user {} by admin {}", user.getEmail(), adminPrincipal.getEmail());
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, UserPrincipal adminPrincipal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Prevent self disabling
        if (user.getId().equals(adminPrincipal.getId()) && request.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Administrators cannot disable their own account.");
        }

        user.setStatus(request.getStatus());
        User saved = userRepository.save(user);

        auditLogService.logPrincipalAction(
                adminPrincipal,
                AuditAction.USER_STATUS_UPDATED,
                "User",
                user.getId().toString(),
                "Status updated to: " + request.getStatus()
        );

        log.info("Status updated for user {} to {} by admin {}", user.getEmail(), request.getStatus(), adminPrincipal.getEmail());
        return userMapper.toResponse(saved);
    }
}
