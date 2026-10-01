package com.contentmanager.modules.ai.service;

import com.contentmanager.modules.ai.dto.AIUsageResponse;
import com.contentmanager.modules.ai.exception.AIRateLimitExceededException;
import com.contentmanager.modules.ai.repository.AIRequestRepository;
import com.contentmanager.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIUsageLimitService Tests")
class AIUsageLimitServiceTest {

    @Mock
    private AIRequestRepository aiRequestRepository;

    private AIUsageLimitService usageLimitService;
    private UserPrincipal adminPrincipal;
    private UserPrincipal authorPrincipal;
    private UserPrincipal viewerPrincipal;

    @BeforeEach
    void setUp() {
        Map<String, Integer> limits = new HashMap<>();
        limits.put("ROLE_ADMIN", 500);
        limits.put("ROLE_EDITOR", 200);
        limits.put("ROLE_AUTHOR", 50);
        limits.put("ROLE_VIEWER", 0);

        usageLimitService = new AIUsageLimitService(aiRequestRepository, 30, limits);

        adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@test.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        authorPrincipal = UserPrincipal.builder()
                .id(2L)
                .email("author@test.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                .build();

        viewerPrincipal = UserPrincipal.builder()
                .id(3L)
                .email("viewer@test.com")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
                .build();
    }

    @Test
    @DisplayName("checkUsageLimit permits user below threshold")
    void checkUsageLimit_underLimit_success() {
        when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(2L), any(Instant.class)))
                .thenReturn(10L);

        assertThatCode(() -> usageLimitService.checkUsageLimit(authorPrincipal))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkUsageLimit throws AIRateLimitExceededException when limit is exceeded")
    void checkUsageLimit_exceeded_throwsException() {
        when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(2L), any(Instant.class)))
                .thenReturn(50L);

        assertThatThrownBy(() -> usageLimitService.checkUsageLimit(authorPrincipal))
                .isInstanceOf(AIRateLimitExceededException.class)
                .hasMessageContaining("limit reached")
                .hasMessageContaining("ROLE_AUTHOR");
    }

    @Test
    @DisplayName("ROLE_VIEWER with zero limit is immediately blocked")
    void checkUsageLimit_viewerZeroLimit_blocked() {
        when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(3L), any(Instant.class)))
                .thenReturn(0L);

        assertThatThrownBy(() -> usageLimitService.checkUsageLimit(viewerPrincipal))
                .isInstanceOf(AIRateLimitExceededException.class);
    }

    @Test
    @DisplayName("getUsageStatus returns accurate usage statistics")
    void getUsageStatus_success() {
        when(aiRequestRepository.countByUserIdAndCreatedAtAfter(eq(1L), any(Instant.class)))
                .thenReturn(25L);

        AIUsageResponse response = usageLimitService.getUsageStatus(adminPrincipal);

        assertThat(response.getUsedToday()).isEqualTo(25L);
        assertThat(response.getDailyLimit()).isEqualTo(500L);
        assertThat(response.getRemainingToday()).isEqualTo(475L);
        assertThat(response.getRole()).isEqualTo("ROLE_ADMIN");
    }
}
