# AniHoyo backend - multi-stage build (Spring Boot 3.4 / Java 17 + FFmpeg)
#
# NOTE: the previous runtime stage used `openjdk:17-slim`, which no longer exists
# on Docker Hub (the official `openjdk` image is deprecated). It is replaced by
# the maintained Eclipse Temurin images.

# ---------------------------------------------------------------- build ----
FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Dependency layer: only invalidated when pom.xml changes.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests -Dmaven.test.skip=true

# ---------------------------------------------------------------- run ----
FROM docker.io/library/eclipse-temurin:17-jre-jammy

# ffmpeg/ffprobe are required for HLS transcoding (hieupham.ffmpeg-path=/usr/bin/ffmpeg).
# curl is used by the container healthcheck and is handy for debugging.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl ca-certificates tini \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Local media root for the non-R2 fallback (hieupham.upload-file.base-uri).
# Also where /storage/** static assets (posters, thumbnails, avatars) are read from.
RUN mkdir -p /uploads

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8083

# The Spring profile is pinned to prod: datasource/redis/JWT/R2 all come from
# environment variables (see docker-compose.yml).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=6 \
    CMD curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8083/api/v1/test-cors \
        | grep -qE '^(200|401|403)$' || exit 1

ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
