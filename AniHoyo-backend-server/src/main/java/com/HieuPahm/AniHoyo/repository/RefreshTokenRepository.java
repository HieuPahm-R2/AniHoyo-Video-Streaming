package com.HieuPahm.AniHoyo.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.HieuPahm.AniHoyo.model.entities.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * JOIN FETCH user để controller còn dùng được thông tin user sau khi
     * transaction của service đã đóng (tránh LazyInitializationException).
     */
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    /**
     * Dọn dòng đã hết hạn hoặc đã thu hồi/xoay từ lâu. Tham số `before` thường là
     * now - retention, nên các phiên vừa xoay vẫn còn trong bảng để phục vụ
     * cửa sổ grace.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before OR rt.revokedAt < :before")
    int deleteStale(@Param("before") Instant before);
}
