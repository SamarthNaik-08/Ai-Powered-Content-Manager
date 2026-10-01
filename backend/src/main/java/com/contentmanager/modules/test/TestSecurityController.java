package com.contentmanager.modules.test;

import com.contentmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Security & RBAC Verification", description = "Endpoints to verify authentication and fine-grained authorization")
public class TestSecurityController {

    @GetMapping("/public")
    @Operation(summary = "Unauthenticated public endpoint")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Public endpoint accessed successfully", "Hello Public"));
    }

    @GetMapping("/protected")
    @Operation(summary = "Authenticated endpoint accessible by any logged-in user")
    public ResponseEntity<ApiResponse<String>> protectedEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Authenticated endpoint accessed successfully", "Hello Authenticated User"));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Role-based endpoint restricted strictly to ROLE_ADMIN")
    public ResponseEntity<ApiResponse<String>> adminOnlyEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Admin-only endpoint accessed successfully", "Hello Admin"));
    }

    @GetMapping("/content-create-permission")
    @PreAuthorize("hasAuthority('CONTENT_CREATE')")
    @Operation(summary = "Permission-based endpoint requiring CONTENT_CREATE authority")
    public ResponseEntity<ApiResponse<String>> contentCreatePermissionEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("CONTENT_CREATE permission verified", "Granted access to create content"));
    }

    @GetMapping("/user-manage-permission")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Permission-based endpoint requiring USER_MANAGE authority")
    public ResponseEntity<ApiResponse<String>> userManagePermissionEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("USER_MANAGE permission verified", "Granted access to manage users"));
    }
}
