package com.HieuPahm.AniHoyo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves {@code /storage/**} straight off the local upload directory.
 *
 * <p>Only active while SeaweedFS storage is off. With SeaweedFS enabled the
 * same URLs are handled by
 * {@link com.HieuPahm.AniHoyo.controller.MediaController}, which streams the
 * objects out of the bucket instead.
 */
@Configuration
@ConditionalOnProperty(prefix = "storage.seaweedfs", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StaticResourceWebConfig implements WebMvcConfigurer {
    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/storage/**").addResourceLocations(baseURI);
 }
}
