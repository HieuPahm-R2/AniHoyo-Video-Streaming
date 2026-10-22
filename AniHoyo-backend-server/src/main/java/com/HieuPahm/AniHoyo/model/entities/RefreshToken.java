package com.HieuPahm.AniHoyo.model.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một phiên đăng nhập = một dòng.
 *
 * Thay cho thiết kế cũ chỉ giữ đúng một refresh token trong `users.refresh_token`:
 * với thiết kế cũ, mỗi lần login ở thiết bị/tab khác (hoặc bất kỳ script nào) đều
 * ghi đè token duy nhất đó, làm cookie của phiên đang chạy thành không hợp lệ và
 * người dùng bị đá về /login dù refresh token còn hạn.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 hex của token thô — DB bị lộ cũng không dùng lại được token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** null = phiên còn hiệu lực; được set khi logout hoặc khi token bị xoay. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Hash của token thay thế. Chỉ khác null khi phiên bị XOAY (không phải logout):
     * dùng để mở "cửa sổ grace" cho tình huống 2 tab cùng refresh một lúc.
     */
    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(Instant.now());
    }

    /** Còn dùng được: chưa logout và chưa hết hạn. */
    public boolean isActiveSession() {
        return !isRevoked() && !isExpired();
    }
}
