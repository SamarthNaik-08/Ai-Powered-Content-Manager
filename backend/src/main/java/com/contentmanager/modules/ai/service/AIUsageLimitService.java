package com.contentmanager.modules.ai.service;

import com.contentmanager.modules.ai.dto.AIUsageResponse;
import com.contentmanager.modules.ai.exception.AIRateLimitExceededException;
import com.contentmanager.modules.ai.repository.AIRequestRepository;
import com.contentmanager.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
public class AIUsageLimitService {

    private final AIRequestRepository aiRequestRepository;
    private final long defaultDailyLimit;
    private final Map<String, Integer> roleDailyLimits;

    public AIUsageLimitService(
            AIRequestRepository aiRequestRepository,
            @Value("${app.ai.limits.default-daily-limit:50}") long defaultDailyLimit,
            @Value("#{${app.ai.limits.role-daily-limits:{ROLE_ADMIN:500,ROLE_EDITOR:200,ROLE_AUTHOR:50,ROLE_VIEWER:0}}}")
            Map<String, Integer> roleDailyLimits) {
        this.aiRequestRepository = aiRequestRepository;
        this.defaultDailyLimit = defaultDailyLimit;
        this.roleDailyLimits = roleDailyLimits;
    }

    public void checkUsageLimit(UserPrincipal principal) {
        if (principal == null) {
            return;
        }

        String primaryRole = resolvePrimaryRole(principal);
        long limit = getLimitForRole(primaryRole);

        // Calculate requests made in the last 24 hours
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        long used = aiRequestRepository.countByUserIdAndCreatedAtAfter(principal.getId(), since);

        if (used >= limit) {
            log.warn("User {} ({}) exceeded daily AI rate limit: {}/{}", principal.getEmail(), primaryRole, used, limit);
            throw new AIRateLimitExceededException(
                    String.format("Daily AI usage limit reached for your role (%s). Allowed: %d requests per day. Used: %d.",
                            primaryRole, limit, used));
        }
    }

    public AIUsageResponse getUsageStatus(UserPrincipal principal) {
        if (principal == null) {
            return AIUsageResponse.builder()
                    .usedToday(0)
                    .dailyLimit(0)
                    .remainingToday(0)
                    .role("ANONYMOUS")
                    .build();
        }

        String primaryRole = resolvePrimaryRole(principal);
        long limit = getLimitForRole(primaryRole);

        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        long used = aiRequestRepository.countByUserIdAndCreatedAtAfter(principal.getId(), since);
        long remaining = Math.max(0, limit - used);

        return AIUsageResponse.builder()
                .usedToday(used)
                .dailyLimit(limit)
                .remainingToday(remaining)
                .role(primaryRole)
                .build();
    }

    private String resolvePrimaryRole(UserPrincipal principal) {
        if (hasRole(principal, "ROLE_ADMIN")) return "ROLE_ADMIN";
        if (hasRole(principal, "ROLE_EDITOR")) return "ROLE_EDITOR";
        if (hasRole(principal, "ROLE_AUTHOR")) return "ROLE_AUTHOR";
        if (hasRole(principal, "ROLE_VIEWER")) return "ROLE_VIEWER";
        return "DEFAULT";
    }

    private boolean hasRole(UserPrincipal principal, String role) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }

    private long getLimitForRole(String role) {
        if (roleDailyLimits != null && roleDailyLimits.containsKey(role)) {
            return roleDailyLimits.get(role);
        }
        return defaultDailyLimit;
    }
}
