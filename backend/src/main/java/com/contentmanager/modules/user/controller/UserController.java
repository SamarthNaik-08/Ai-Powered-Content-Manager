package com.contentmanager.modules.user.controller;

import com.contentmanager.common.ApiResponse;
import com.contentmanager.modules.user.dto.UpdateUserRoleRequest;
import com.contentmanager.modules.user.dto.UpdateUserStatusRequest;
import com.contentmanager.modules.user.dto.UserResponse;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.service.UserService;
import com.contentmanager.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Administration of system users, status toggles, and RBAC role assignments")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get currently authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search and filter system users with pagination (Admin only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserResponse> page = userService.searchUsers(search, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Operation(summary = "Get user details by ID (Admin only)")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Update user security roles (Admin only)")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        UserResponse user = userService.updateUserRoles(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", user));
    }

    @Operation(summary = "Update user account status: ACTIVE or SUSPENDED (Admin only)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        UserResponse user = userService.updateUserStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }
}
