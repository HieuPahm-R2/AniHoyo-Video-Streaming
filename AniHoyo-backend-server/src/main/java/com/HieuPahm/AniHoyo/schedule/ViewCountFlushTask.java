package com.HieuPahm.AniHoyo.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.HieuPahm.AniHoyo.services.implement.SeasonService;

/** Flushes Redis view counters to MySQL on a fixed delay instead of per view. */
@Slf4j
@Component
public class ViewCountFlushTask {
    private final SeasonService seasonService;

    public ViewCountFlushTask(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @Scheduled(fixedDelayString = "${anihoyo.views.flush-interval-ms:300000}")
    public void flush() {
        try {
            seasonService.flushPendingViewCounts();
        } catch (RuntimeException exception) {
            log.error("Unable to flush pending view counters; they will be retried", exception);
        }
    }
}
