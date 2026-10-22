package com.HieuPahm.AniHoyo.services.implement;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.HieuPahm.AniHoyo.config.R2StorageConfiguration;
import com.HieuPahm.AniHoyo.config.R2StorageProperties;
import com.HieuPahm.AniHoyo.model.entities.Episode;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2MediaStorageService {
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final R2StorageProperties properties;

    public R2MediaStorageService(
            @Qualifier(R2StorageConfiguration.S3_CLIENT_BEAN) ObjectProvider<S3Client> s3ClientProvider,
            R2StorageProperties properties) {
        this.s3ClientProvider = s3ClientProvider;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Stores originals only in the private source bucket. */
    public String storeSourceVideo(MultipartFile file) throws IOException {
        requireEnabled();
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String key = "raw/" + UUID.randomUUID() + "-" + originalName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getSourceBucket())
                .key(key)
                .contentType(contentType(file.getContentType(), originalName))
                .contentLength(file.getSize())
                .build();

        try (InputStream input = file.getInputStream()) {
            client().putObject(request, RequestBody.fromInputStream(input, file.getSize()));
        }
        return key;
    }

    /** Downloads a private original to a short-lived local file for FFmpeg. */
    public Path downloadSourceVideo(String key) throws IOException {
        requireEnabled();
        String suffix = suffixFromKey(key);
        Path temporaryFile = Files.createTempFile("anihoyo-r2-source-", suffix);
        try (ResponseInputStream<GetObjectResponse> input = client().getObject(GetObjectRequest.builder()
                .bucket(properties.getSourceBucket())
                .key(key)
                .build())) {
            Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            return temporaryFile;
        } catch (RuntimeException | IOException exception) {
            Files.deleteIfExists(temporaryFile);
            throw exception;
        }
    }

    /** Uploads immutable HLS output to the public media bucket. */
    public void uploadHlsDirectory(Episode episode, Path hlsRoot) throws IOException {
        requireEnabled();
        try (Stream<Path> paths = Files.walk(hlsRoot)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                String relativeKey = hlsRoot.relativize(file).toString().replace('\\', '/');
                String key = hlsPrefix(episode) + "/" + relativeKey;
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(properties.getMediaBucket())
                        .key(key)
                        .contentType(contentType(null, relativeKey))
                        .cacheControl("public, max-age=31536000, immutable")
                        .build();
                client().putObject(request, RequestBody.fromFile(file));
            }
        }
    }

    public URI masterPlaylistUri(Episode episode) {
        return objectUri(episode, "master.m3u8");
    }

    public URI objectUri(Episode episode, String relativePath) {
        String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
        return URI.create(baseUrl + "/" + hlsPrefix(episode) + "/" + relativePath);
    }

    /** When false the API redirects to the CDN instead of relaying the bytes. */
    public boolean isRelayEnabled() {
        return properties.isHlsRelay();
    }

    /**
     * Opens one HLS object from the media bucket as a Spring {@link Resource}.
     *
     * <p>The relay exists because the CDN lives on another origin: serving the
     * playlists and segments from the API keeps playback same-origin, so no CORS
     * policy is needed on the bucket and hls.js works in every browser.
     *
     * @return the resource, or {@code null} when the object is not in the bucket
     */
    public Resource mediaResource(Episode episode, String relativePath) {
        requireEnabled();
        String key = hlsPrefix(episode) + "/" + relativePath;
        try {
            HeadObjectResponse head = client().headObject(HeadObjectRequest.builder()
                    .bucket(properties.getMediaBucket())
                    .key(key)
                    .build());
            return new R2ObjectResource(key, head.contentLength());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    /** Lazily opened S3 object: the length is known up-front, the body on first read. */
    private final class R2ObjectResource extends AbstractResource {
        private final String key;
        private final long length;

        private R2ObjectResource(String key, long length) {
            this.key = key;
            this.length = length;
        }

        @Override
        public String getDescription() {
            return "R2 object [" + key + "]";
        }

        @Override
        public InputStream getInputStream() {
            return client().getObject(GetObjectRequest.builder()
                    .bucket(properties.getMediaBucket())
                    .key(key)
                    .build());
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

    public void deleteDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary files are best effort; never mask the encoding result.
                }
            });
        } catch (IOException ignored) {
            // Temporary files are best effort; never mask the encoding result.
        }
    }

    private S3Client client() {
        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("R2 storage is not configured.");
        }
        return client;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("R2 storage is disabled.");
        }
    }

    private String hlsPrefix(Episode episode) {
        if (episode.getId() <= 0 || !StringUtils.hasText(episode.getFilePath())) {
            throw new IllegalArgumentException("Episode id and source file key are required for HLS storage.");
        }
        return trimSlashes(properties.getHlsPrefix()) + "/episodes/" + episode.getId() + "/"
                + sha256(episode.getFilePath()).substring(0, 16);
    }

    private String sanitizeFilename(String filename) {
        String safe = StringUtils.hasText(filename) ? filename.replaceAll("[^a-zA-Z0-9.\\-]", "_") : "video.bin";
        return safe.isBlank() ? "video.bin" : safe;
    }

    private String suffixFromKey(String key) {
        int index = key.lastIndexOf('.');
        return index >= 0 ? key.substring(index) : ".bin";
    }

    private String contentType(String provided, String filename) {
        if (StringUtils.hasText(provided)) {
            return provided;
        }
        String name = filename.toLowerCase();
        if (name.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (name.endsWith(".ts")) return "video/mp2t";
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }

    private String trimSlashes(String value) {
        return value == null ? "hls" : value.replaceAll("^/+|/+$", "");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
