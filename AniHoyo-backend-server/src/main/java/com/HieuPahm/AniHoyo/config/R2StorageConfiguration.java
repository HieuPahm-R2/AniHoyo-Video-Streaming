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

@Configuration
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfiguration {

    /**
     * Bean name for the R2 client. It must stay distinct from the SeaweedFS one
     * because both are {@link S3Client} instances and injection is by qualifier.
     */
    public static final String S3_CLIENT_BEAN = "r2S3Client";

    @Bean(S3_CLIENT_BEAN)
    @ConditionalOnProperty(prefix = "storage.r2", name = "enabled", havingValue = "true")
    S3Client r2S3Client(R2StorageProperties properties) {
        if (!StringUtils.hasText(properties.getAccountId())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getSecretAccessKey())) {
            throw new IllegalStateException(
                    "R2 is enabled but R2_ACCOUNT_ID, access key, or secret key is missing. "
                            + "Fill them in the shared ../.env file, or set R2_ENABLED=false.");
        }

        String endpoint = StringUtils.hasText(properties.getEndpoint())
                ? properties.getEndpoint()
                : "https://" + properties.getAccountId() + ".r2.cloudflarestorage.com";

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
