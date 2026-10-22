package com.HieuPahm.AniHoyo.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.HieuPahm.AniHoyo.services.IRefreshTokenService;

import lombok.extern.slf4j.Slf4j;

/** Dọn các dòng refresh_tokens đã hết hạn để bảng không phình ra. */
@Slf4j
@Component
public class RefreshTokenCleanupTask {

    private final IRefreshTokenService refreshTokenService;

    public RefreshTokenCleanupTask(IRefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "${anihoyo.jwt.refresh-cleanup-cron:0 30 3 * * *}")
    public void purge() {
        try {
            this.refreshTokenService.purgeStale();
        } catch (RuntimeException exception) {
            log.error("Không dọn được refresh_tokens; sẽ thử lại ở lần chạy sau", exception);
        }
    }
}
