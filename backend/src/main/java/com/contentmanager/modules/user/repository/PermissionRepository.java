package com.contentmanager.modules.user.repository;

import com.contentmanager.modules.rbac.PermissionName;
import com.contentmanager.modules.user.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(PermissionName name);
}
