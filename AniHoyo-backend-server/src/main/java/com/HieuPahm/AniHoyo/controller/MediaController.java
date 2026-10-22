package com.HieuPahm.AniHoyo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import com.HieuPahm.AniHoyo.services.implement.SeaweedFSMediaStorageService;

import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Serves {@code GET /storage/**} from SeaweedFS (posters, thumbnails, avatars,
 * sliders), replacing the previous "read the container's local disk" resource
 * handler.
 *
 * <p>The URL shape is unchanged: {@code /storage/<folder>/<file>} maps to the
 * SeaweedFS object key {@code <folder>/<file>}, so the frontend, the database
 * columns and the nginx proxy config all keep working without edits.
 *
 * <p>This bean is only active while {@code storage.seaweedfs.enabled=true};
 * otherwise {@link com.HieuPahm.AniHoyo.config.StaticResourceWebConfig} keeps
 * serving from the local volume.
 */
@RestController
@ConditionalOnProperty(prefix = "storage.seaweedfs", name = "enabled", havingValue = "true")
public class MediaController {
    private static final Logger log = LoggerFactory.getLogger(MediaController.class);
    private static final String PREFIX = "/storage/";

    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;

    private final SeaweedFSMediaStorageService storage;

    public MediaController(SeaweedFSMediaStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/storage/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String key = storageKey(request);
        if (key == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<HeadObjectResponse> metadata = storage.head(key);
        if (metadata.isPresent()) {
            HeadObjectResponse head = metadata.get();
            ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                    .contentType(resolveContentType(head.contentType(), key))
                    .contentLength(head.contentLength())
                    // Filenames are timestamp-prefixed and never rewritten in place.
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic());
            if (StringUtils.hasText(head.eTag())) {
                response.header(HttpHeaders.ETAG, head.eTag());
            }
            return response.body(new SeaweedObjectResource(storage, key, head.contentLength()));
        }

        Resource local = localFallback(key);
        if (local != null) {
            return ResponseEntity.ok()
                    .contentType(resolveContentType(null, key))
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
                    .body(local);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Resolves the object key from the request path.
     *
     * <p>Returns {@code null} when the path could escape the storage root,
     * which would otherwise let a caller read arbitrary container files through
     * the local fallback. Only whole {@code .}/{@code ..} segments are rejected,
     * so a legitimate name such as {@code 1735-a..png} still resolves.
     */
    private String storageKey(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String path = attribute != null ? attribute.toString() : request.getRequestURI();
        if (!path.startsWith(PREFIX)) {
            return null;
        }
        String key = path.substring(PREFIX.length());
        if (!StringUtils.hasText(key) || key.startsWith("/") || key.indexOf('\0') >= 0) {
            return null;
        }
        for (String segment : key.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }
        return key;
    }

    /** Pre-migration objects still sitting in the mounted upload directory. */
    private Resource localFallback(String key) {
        if (!storage.isFallbackToLocal()) {
            return null;
        }
        Path root;
        try {
            root = Paths.get(URI.create(baseURI)).toAbsolutePath().normalize();
        } catch (IllegalArgumentException exception) {
            return null;
        }
        Path path = root.resolve(key).normalize();
        // Belt and braces on top of storageKey(): never read outside the root.
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return null;
        }
        log.debug("Serving '{}' from the local volume; it has not been migrated to SeaweedFS yet.", key);
        return new FileSystemResource(path);
    }

    private MediaType resolveContentType(String provided, String key) {
        if (StringUtils.hasText(provided)) {
            try {
                return MediaType.parseMediaType(provided);
            } catch (IllegalArgumentException ignored) {
                // Fall through to the extension-based guess.
            }
        }
        String name = key.toLowerCase();
        if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (name.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (name.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (name.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** Streams straight out of SeaweedFS without buffering the object in the heap. */
    private static final class SeaweedObjectResource extends AbstractResource {
        private final SeaweedFSMediaStorageService storage;
        private final String key;
        private final long length;

        private SeaweedObjectResource(SeaweedFSMediaStorageService storage, String key, long length) {
            this.storage = storage;
            this.key = key;
            this.length = length;
        }

        @Override
        public String getDescription() {
            return "SeaweedFS object [" + key + "]";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return storage.open(key);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public String getFilename() {
            return key;
        }
    }
}
