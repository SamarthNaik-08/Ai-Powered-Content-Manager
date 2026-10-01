package com.contentmanager.modules.user.dto;

import com.contentmanager.modules.user.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private UUID uuid;
    private String name;
    private String email;
    private UserStatus status;
    private Set<String> roles;
    private Set<String> permissions;
    private Instant createdAt;
    private Instant lastLoginAt;
}
