package com.contentmanager.modules.user.mapper;

import com.contentmanager.modules.user.dto.UserResponse;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleNames")
    @Mapping(target = "permissions", source = "roles", qualifiedByName = "mapPermissionNames")
    UserResponse toResponse(User user);

    @Named("mapRoleNames")
    default Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

    @Named("mapPermissionNames")
    default Set<String> mapPermissionNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName().name())
                .collect(Collectors.toSet());
    }
}
