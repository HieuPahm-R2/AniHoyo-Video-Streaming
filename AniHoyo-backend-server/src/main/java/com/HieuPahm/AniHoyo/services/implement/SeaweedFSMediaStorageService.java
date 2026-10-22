package com.HieuPahm.AniHoyo.services.implement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.HieuPahm.AniHoyo.config.SeaweedFSStorageConfiguration;
import com.HieuPahm.AniHoyo.config.SeaweedFSStorageProperties;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Stores and serves the small web assets (posters, thumbnails, avatars,
 * sliders) on SeaweedFS instead of the backend container's local disk.
 *
 * <p>Objects are addressed by a relative key that mirrors the public URL, e.g.
 * {@code visual/1735689600000-poster.png} is served at
 * {@code /storage/visual/1735689600000-poster.png}. That keeps every existing
 * URL, database column and frontend reference valid.
 *
 * <p>Videos are deliberately out of scope: originals and HLS output stay on R2.
 */
@Service
public class SeaweedFSMediaStorageService {
    private static final Logger log = LoggerFactory.getLogger(SeaweedFSMediaStorageService.class);

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final SeaweedFSStorageProperties properties;

    public SeaweedFSMediaStorageService(
            @Qualifier(SeaweedFSStorageConfiguration.S3_CLIENT_BEAN) ObjectProvider<S3Client> s3ClientProvider,
            SeaweedFSStorageProperties properties) {
        this.s3ClientProvider = s3ClientProvider;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Whether a missing object may still be read from the legacy local volume. */
    public boolean isFallbackToLocal() {
        return properties.isFallbackToLocal();
    }

    public String bucket() {
        return properties.getBucket();
    }

    /**
     * Creates the bucket when it is missing. SeaweedFS starts empty, and the
     * migration script as well as the first upload both assume it exists.
     *
     * <p>The S3 gateway comes up a moment after the master it is healthchecked
     * on, so a few retries keep container startup ordering from turning into a
     * crash loop.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        if (!isEnabled()) {
            return;
        }
        SdkException lastFailure = null;
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                client().headBucket(HeadBucketRequest.builder().bucket(bucket()).build());
                log.info("SeaweedFS bucket '{}' is ready.", bucket());
                return;
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) {
                    try {
                        client().createBucket(CreateBucketRequest.builder().bucket(bucket()).build());
                        log.info("SeaweedFS bucket '{}' created.", bucket());
                        return;
                    } catch (SdkException createFailure) {
                        lastFailure = createFailure;
                    }
                } else {
                    lastFailure = exception;
                }
            } catch (SdkException exception) {
                // Connection refused / endpoint not resolvable: the gateway may
                // still be starting up, so retry rather than abort.
                lastFailure = exception;
            }

            log.warn("SeaweedFS bucket '{}' not ready (attempt {}/10): {}",
                    bucket(), attempt, lastFailure.getMessage());
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for SeaweedFS", interrupted);
            }
        }
        throw new IllegalStateException(
                "SeaweedFS bucket '" + bucket() + "' is not available: " + lastFailure.getMessage(), lastFailure);
    }

    /** Uploads one object under {@code key}; bytes are streamed, never buffered. */
    public void put(MultipartFile file, String key) throws IOException {
        requireEnabled();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .contentType(StringUtils.hasText(file.getContentType())
                        ? file.getContentType()
                        : "application/octet-stream")
                .contentLength(file.getSize())
                .build();
        try (InputStream input = file.getInputStream()) {
            client().putObject(request, RequestBody.fromInputStream(input, file.getSize()));
        }
    }

    /** Metadata for {@code key}, or empty when the object does not exist. */
    public Optional<HeadObjectResponse> head(String key) {
        requireEnabled();
        try {
            return Optional.of(client().headObject(HeadObjectRequest.builder()
                    .bucket(bucket())
                    .key(key)
                    .build()));
        } catch (NoSuchKeyException exception) {
            return Optional.empty();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * Opens {@code key} for reading. The caller owns the stream and must close
     * it; Spring closes it once the response has been written.
     */
    public ResponseInputStream<GetObjectResponse> open(String key) {
        requireEnabled();
        return client().getObject(GetObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .build());
    }

    private S3Client client() {
        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("SeaweedFS storage is not configured.");
        }
        return client;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("SeaweedFS storage is disabled.");
        }
    }
}
