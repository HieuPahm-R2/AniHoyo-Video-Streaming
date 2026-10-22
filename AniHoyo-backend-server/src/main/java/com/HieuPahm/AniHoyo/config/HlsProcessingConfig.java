package com.HieuPahm.AniHoyo.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool used by the HLS pipeline ({@code EpisodeService#processVideo}).
 *
 * <p>One worker on purpose: FFmpeg already saturates every core it is given, and
 * the podman machine is small, so running two transcodes at once only makes both
 * slower. Extra uploads wait in a bounded queue instead of spawning threads.
 */
@Configuration
public class HlsProcessingConfig {

    public static final String HLS_EXECUTOR = "hlsTaskExecutor";

    @Bean(name = HLS_EXECUTOR)
    public Executor hlsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("hls-");
        // A restart must not block on a half-finished transcode: the episode stays
        // in PENDING/PROCESSING and HlsStartupRecovery re-queues it after boot.
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
