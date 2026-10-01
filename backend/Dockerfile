# ==============================================================================
# Multi-stage Dockerfile for AI-Powered Content Management Platform Backend
# ==============================================================================

# Stage 1: Build JAR artifact
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal hardened production JRE runtime
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Install security updates and utilities for container healthchecks
RUN apk update && apk upgrade && apk add --no-cache curl wget bash && rm -rf /var/cache/apk/*

# Create dedicated non-root application user and group
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create and grant permissions for persistent media uploads directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/ai-powered-content-manager-backend-*.jar app.jar

# Run as unprivileged user
USER appuser:appgroup

# Container execution and memory tuning for Kubernetes/Docker environments
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# Health check using Spring Boot Actuator liveness probe
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
