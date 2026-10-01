package com.contentmanager.config;

import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.RoleRepository;
import com.contentmanager.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeDefaultAdmin();
    }

    private void initializeDefaultAdmin() {
        String adminEmail = "admin@contentmanager.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(RoleName.ROLE_ADMIN)
                            .description("Default Administrator Role")
                            .build()));

            User admin = User.builder()
                    .uuid(UUID.randomUUID())
                    .name("System Administrator")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .status(UserStatus.ACTIVE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("Initialized default system administrator user: {}", adminEmail);
        }
    }
}
