package com.HieuPahm.AniHoyo.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Builds the S3 client that talks to the SeaweedFS S3 gateway.
 *
 * <p>The bean is explicitly named/qualified so it can coexist with the R2 client:
 * both are {@link S3Client} instances but they point at different endpoints.
 */
@Configuration
@EnableConfigurationProperties(SeaweedFSStorageProperties.class)
public class SeaweedFSStorageConfiguration {

    public static final String S3_CLIENT_BEAN = "seaweedfsS3Client";

    @Bean(S3_CLIENT_BEAN)
    @ConditionalOnProperty(prefix = "storage.seaweedfs", name = "enabled", havingValue = "true")
    S3Client seaweedfsS3Client(SeaweedFSStorageProperties properties) {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getSecretAccessKey())
                || !StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException(
                    "SeaweedFS is enabled but SEAWEEDFS_ENDPOINT, SEAWEEDFS_ACCESS_KEY_ID, "
                            + "SEAWEEDFS_SECRET_ACCESS_KEY or SEAWEEDFS_BUCKET is missing.");
        }

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())))
                // SeaweedFS serves buckets as a path segment, not as a subdomain.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
