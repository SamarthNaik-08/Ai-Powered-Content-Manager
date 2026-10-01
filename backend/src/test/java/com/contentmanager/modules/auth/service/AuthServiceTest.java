package com.contentmanager.modules.auth.service;

import com.contentmanager.common.exception.DuplicateResourceException;
import com.contentmanager.common.exception.InvalidCredentialsException;
import com.contentmanager.modules.auth.dto.AuthResponse;
import com.contentmanager.modules.auth.dto.LoginRequest;
import com.contentmanager.modules.auth.dto.RefreshTokenRequest;
import com.contentmanager.modules.auth.dto.RegisterRequest;
import com.contentmanager.modules.auth.model.RefreshToken;
import com.contentmanager.modules.auth.repository.RefreshTokenRepository;
import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.dto.UserResponse;
import com.contentmanager.modules.user.mapper.UserMapper;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import com.contentmanager.modules.user.repository.RoleRepository;
import com.contentmanager.modules.user.repository.UserRepository;
import com.contentmanager.security.JwtTokenProvider;
import com.contentmanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.contentmanager.modules.audit.service.AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role authorRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);

        authorRole = Role.builder()
                .id(1L)
                .name(RoleName.ROLE_AUTHOR)
                .build();

        sampleUser = User.builder()
                .id(10L)
                .uuid(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .passwordHash("hashed_password_123")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(authorRole))
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password and default AUTHOR role")
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("SecurePassword@123")
                .build();

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_AUTHOR)).thenReturn(Optional.of(authorRole));
        when(passwordEncoder.encode("SecurePassword@123")).thenReturn("hashed_password_123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("jwt-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse mockResponse = UserResponse.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .roles(Set.of("ROLE_AUTHOR"))
                .build();
        when(userMapper.toResponse(any(User.class))).thenReturn(mockResponse);

        AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("jane@example.com");
        verify(passwordEncoder).encode("SecurePassword@123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email is already taken")
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .password("SecurePassword@123")
                .build();

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate user and return token pair on valid credentials")
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("SecurePassword@123")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.findByEmailWithRolesAndPermissions("jane@example.com")).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateAccessToken(any())).thenReturn("jwt-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(sampleUser)).thenReturn(UserResponse.builder().email("jane@example.com").build());

        AuthResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(result.getRefreshToken()).isNotBlank();
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException on bad credentials")
    void shouldThrowOnInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should rotate refresh token on valid refresh request")
    void shouldRotateRefreshTokenSuccessfully() {
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token("valid-old-token")
                .user(sampleUser)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("valid-old-token")).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(userMapper.toResponse(sampleUser)).thenReturn(UserResponse.builder().email("jane@example.com").build());

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("valid-old-token"));

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(validToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldRevokeTokenOnLogout() {
        authService.logout("token-to-revoke");
        verify(refreshTokenRepository).revokeToken("token-to-revoke");
    }
}
