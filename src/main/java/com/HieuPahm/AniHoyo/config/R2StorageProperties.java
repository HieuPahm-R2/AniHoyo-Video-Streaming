package com.HieuPahm.AniHoyo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.r2")
public class R2StorageProperties {
    private boolean enabled;
    private String accountId;
    private String endpoint;
    private String accessKeyId;
    private String secretAccessKey;
    private String sourceBucket;
    private String mediaBucket;
    private String publicBaseUrl;
    private String hlsPrefix;

    /**
     * {@code true} (default): the API streams HLS playlists/segments itself, so
     * playback stays same-origin and needs no CORS rule on the bucket.
     * {@code false}: answer 302 to {@link #publicBaseUrl} and let the CDN serve
     * the media — cheaper, but the bucket then needs a CORS policy allowing the
     * SPA origin, otherwise hls.js cannot read the playlists in a browser.
     */
    private boolean hlsRelay = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public void setSecretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; }
    public String getSourceBucket() { return sourceBucket; }
    public void setSourceBucket(String sourceBucket) { this.sourceBucket = sourceBucket; }
    public String getMediaBucket() { return mediaBucket; }
    public void setMediaBucket(String mediaBucket) { this.mediaBucket = mediaBucket; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getHlsPrefix() { return hlsPrefix; }
    public void setHlsPrefix(String hlsPrefix) { this.hlsPrefix = hlsPrefix; }
    public boolean isHlsRelay() { return hlsRelay; }
    public void setHlsRelay(boolean hlsRelay) { this.hlsRelay = hlsRelay; }
}
