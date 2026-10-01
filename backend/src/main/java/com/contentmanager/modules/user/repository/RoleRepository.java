package com.contentmanager.modules.user.repository;

import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
