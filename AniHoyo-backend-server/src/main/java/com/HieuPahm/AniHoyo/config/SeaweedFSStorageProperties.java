package com.HieuPahm.AniHoyo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code storage.seaweedfs.*}.
 *
 * <p>SeaweedFS stores the small, immutable web assets (posters, thumbnails,
 * avatars, sliders) that used to live on the backend container's local disk.
 * Videos and their HLS output intentionally stay on Cloudflare R2, which is
 * exposed to viewers through the CDN.
 */
@ConfigurationProperties(prefix = "storage.seaweedfs")
public class SeaweedFSStorageProperties {
    private boolean enabled;
    /** S3 gateway endpoint, e.g. {@code http://seaweedfs:8333}. */
    private String endpoint;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucket;
    private String region = "us-east-1";
    /**
     * When true, a missing object is looked up in {@code hieupham.upload-file.base-uri}
     * instead of returning 404. Keeps assets uploaded before the migration usable.
     */
    private boolean fallbackToLocal = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public boolean isFallbackToLocal() { return fallbackToLocal; }
    public void setFallbackToLocal(boolean fallbackToLocal) { this.fallbackToLocal = fallbackToLocal; }
}
