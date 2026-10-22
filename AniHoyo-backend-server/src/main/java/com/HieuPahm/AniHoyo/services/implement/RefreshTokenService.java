package com.HieuPahm.AniHoyo.services.implement;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.HieuPahm.AniHoyo.model.entities.RefreshToken;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.repository.RefreshTokenRepository;
import com.HieuPahm.AniHoyo.services.IRefreshTokenService;
import com.HieuPahm.AniHoyo.utils.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Quản lý phiên đăng nhập qua bảng `refresh_tokens` (mỗi phiên một dòng) thay cho
 * thiết kế cũ chỉ giữ một refresh token trong `users.refresh_token` — thiết kế cũ
 * khiến mọi lần login ở nơi khác là cookie của phiên đang chạy bị vô hiệu hoá.
 */
@Slf4j
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Token vừa bị xoay còn dùng được thêm bao lâu (2 tab refresh cùng lúc). */
    @Value("${anihoyo.jwt.refresh-grace-seconds:60}")
    private long graceSeconds;

    /** Giữ dòng đã thu hồi/hết hạn thêm bao lâu trước khi dọn hẳn. */
    @Value("${anihoyo.jwt.refresh-retention-hours:24}")
    private long retentionHours;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken create(String rawToken, User user, long validForSeconds, String userAgent) {
        Instant now = Instant.now();
        RefreshToken session = new RefreshToken();
        session.setTokenHash(SecurityUtils.hashToken(rawToken));
        session.setUser(user);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusSeconds(validForSeconds));
        session.setUserAgent(truncate(userAgent, 255));
        return this.refreshTokenRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        RefreshToken session = this.refreshTokenRepository
                .findByTokenHashWithUser(SecurityUtils.hashToken(rawToken))
                .orElse(null);
        if (session == null || session.isExpired()) {
            return null;
        }
        if (session.isRevoked() && !isWithinRotationGrace(session)) {
            // Đã logout, hoặc bị xoay từ lâu (token cũ bị dùng lại) → coi như hết phiên.
            return null;
        }
        return session;
    }

    @Override
    @Transactional
    public RefreshToken rotate(RefreshToken current, String newRawToken, long validForSeconds, String userAgent) {
        if (current.getRevokedAt() == null) {
            current.setRevokedAt(Instant.now());
            current.setReplacedByHash(SecurityUtils.hashToken(newRawToken));
            this.refreshTokenRepository.save(current);
        }
        return create(newRawToken, current.getUser(), validForSeconds, userAgent);
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        this.refreshTokenRepository.findByTokenHashWithUser(SecurityUtils.hashToken(rawToken))
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        // Không set replacedByHash ⇒ phiên này không rơi vào cửa sổ grace:
                        // logout là mất hiệu lực ngay.
                        session.setRevokedAt(Instant.now());
                        this.refreshTokenRepository.save(session);
                    }
                });
    }

    @Override
    @Transactional
    public int purgeStale() {
        Instant before = Instant.now().minus(Duration.ofHours(Math.max(retentionHours, 1)));
        int removed = this.refreshTokenRepository.deleteStale(before);
        if (removed > 0) {
            log.info("Đã dọn {} refresh token hết hạn/đã thu hồi", removed);
        }
        return removed;
    }

    private boolean isWithinRotationGrace(RefreshToken session) {
        return session.getReplacedByHash() != null
                && session.getRevokedAt() != null
                && session.getRevokedAt().plusSeconds(Math.max(graceSeconds, 0)).isAfter(Instant.now());
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
