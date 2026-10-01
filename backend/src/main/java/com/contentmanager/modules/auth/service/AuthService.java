package com.contentmanager.modules.auth.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.InvalidCredentialsException;
import com.contentmanager.common.exception.TokenRefreshException;
import com.contentmanager.modules.audit.model.AuditAction;
import com.contentmanager.modules.audit.service.AuditLogService;
import com.contentmanager.modules.auth.dto.AuthResponse;
import com.contentmanager.modules.auth.dto.LoginRequest;
import com.contentmanager.modules.auth.dto.RefreshTokenRequest;
import com.contentmanager.modules.auth.dto.RegisterRequest;
import com.contentmanager.modules.auth.model.RefreshToken;
import com.contentmanager.modules.auth.repository.RefreshTokenRepository;
import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.mapper.UserMapper;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.RoleRepository;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.JwtTokenProvider;
import com.contentmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("An account with this email address already exists.");
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_AUTHOR)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_AUTHOR)
                        .description("Default Author Role")
                        .build()));

        User user = User.builder()
                .uuid(UUID.randomUUID())
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(defaultRole)))
                .lastLoginAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email: {} and UUID: {}", savedUser.getEmail(), savedUser.getUuid());

        auditLogService.logAction(savedUser, savedUser.getEmail(), AuditAction.USER_CREATED, "User", savedUser.getId().toString(), "127.0.0.1", "Web Client", "User registered");

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String accessToken = tokenProvider.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationInMs())
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmailWithRolesAndPermissions(principal.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditLogService.logAction(user, user.getEmail(), AuditAction.LOGIN, "Auth", user.getId().toString(), "127.0.0.1", "Web Client", "Login successful");

        String accessToken = tokenProvider.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User {} successfully authenticated.", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationInMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new TokenRefreshException("Refresh token was not found in the database."));

        if (!refreshToken.isActive()) {
            refreshTokenRepository.revokeToken(tokenStr);
            throw new TokenRefreshException("Refresh token has expired or been revoked. Please sign in again.");
        }

        User user = refreshToken.getUser();
        // Revoke used refresh token for strict rotation security
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationInMs())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.revokeToken(refreshTokenStr.trim());
            auditLogService.logSystemAction(AuditAction.LOGOUT, "Auth", null, "User logout executed");
            log.info("Revoked refresh token on logout.");
        }
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }
}
