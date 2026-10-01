# ==============================================================================
# Multi-stage Dockerfile for AI-Powered Content Management Platform Frontend
# ==============================================================================

# Stage 1: Build production static bundle
FROM node:20-alpine AS builder
WORKDIR /workspace

# Install dependencies with frozen lockfile
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build Vite production bundle
COPY . .
RUN npm run build

# Stage 2: Hardened Nginx production web server
FROM nginx:1.27-alpine AS runner

# Install wget for container health checks
RUN apk update && apk upgrade && apk add --no-cache wget && rm -rf /var/cache/apk/*

# Copy built frontend assets
COPY --from=builder /workspace/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Health check probing Nginx health endpoint
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
