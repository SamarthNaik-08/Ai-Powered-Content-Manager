package com.contentmanager.security;

import com.contentmanager.modules.rbac.RoleName;
import com.contentmanager.modules.user.model.Role;
import com.contentmanager.modules.user.model.User;
import com.contentmanager.modules.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UserPrincipal samplePrincipal;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 3600000L);
        tokenProvider.init();

        Role authorRole = Role.builder().name(RoleName.ROLE_AUTHOR).build();
        User user = User.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .name("Alex Smith")
                .email("alex@example.com")
                .passwordHash("secret_hash")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(authorRole))
                .build();

        samplePrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse username correctly")
    void shouldGenerateAndValidateToken() {
        String token = tokenProvider.generateAccessToken(samplePrincipal);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("alex@example.com");
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void shouldRejectInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidpayload.invalidsignature";
        assertThat(tokenProvider.validateToken(invalidToken)).isFalse();
    }
}
