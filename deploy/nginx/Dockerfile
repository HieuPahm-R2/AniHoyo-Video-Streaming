# AniHoyo web tier: build the React SPA, then serve it with Nginx.
#
# Build context is the REPOSITORY ROOT (see docker-compose.yml) so that both the
# frontend sources and deploy/nginx/* configuration are reachable.
#
#   podman build -f deploy/nginx/Dockerfile -t anihoyo-web .

#--- build ----
FROM docker.io/library/node:22-alpine AS spa

WORKDIR /app

# Dependency layer (cached unless the lockfile changes).
COPY AniHoyo-frontend-src/package.json AniHoyo-frontend-src/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY AniHoyo-frontend-src/ ./

# The SPA is served from the same origin as the API, so the backend URL must be
# an EMPTY string: every request becomes relative ("/api/v1/...", "/storage/...")
# and is proxied by Nginx. Using an absolute URL would trigger CORS failures,
# because the backend only allows the Vite dev origins (3000/4173/5173).
ARG VITE_BACKEND_URL=""
ARG VITE_ACL_ENABLE="true"
ENV VITE_BACKEND_URL=${VITE_BACKEND_URL} \
    VITE_ACL_ENABLE=${VITE_ACL_ENABLE}

RUN npm run build

# ---------------------------------------------------------------- serve ----
FROM docker.io/library/nginx:1.27-alpine AS web

RUN rm -f /etc/nginx/conf.d/default.conf

COPY deploy/nginx/nginx.conf      /etc/nginx/nginx.conf
COPY deploy/nginx/conf.d/         /etc/nginx/conf.d/
COPY deploy/nginx/snippets/       /etc/nginx/snippets/
COPY --from=spa /app/dist/        /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://127.0.0.1/healthz >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
