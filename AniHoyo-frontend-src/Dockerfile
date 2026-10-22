# Stage 1: Build Angular application
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build application for production
RUN npm run build

# Stage 2: Serve with Caddy
FROM caddy:2-alpine

WORKDIR /srv

# Copy built static files
COPY --from=build /app/dist/frontend-angular/browser /srv

# Copy Caddyfile for serving static files
COPY Caddyfile.frontend /etc/caddy/Caddyfile

# Expose port 80
EXPOSE 80

# Caddy runs as non-root user by default
CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]
