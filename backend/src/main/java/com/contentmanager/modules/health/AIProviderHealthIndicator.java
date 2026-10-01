package com.contentmanager.modules.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AIProviderHealthIndicator implements HealthIndicator {

    @Value("${app.ai.provider:mock}")
    private String providerName;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Override
    public Health health() {
        if ("mock".equalsIgnoreCase(providerName)) {
            return Health.up()
                    .withDetail("provider", "mock")
                    .withDetail("model", model)
                    .withDetail("status", "Local development mock provider active")
                    .build();
        }

        return Health.up()
                .withDetail("provider", providerName)
                .withDetail("model", model)
                .withDetail("status", "External provider configured")
                .build();
    }
}
