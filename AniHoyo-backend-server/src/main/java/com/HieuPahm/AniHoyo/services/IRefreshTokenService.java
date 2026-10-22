package com.HieuPahm.AniHoyo.services;

import com.HieuPahm.AniHoyo.model.entities.RefreshToken;
import com.HieuPahm.AniHoyo.model.entities.User;

/**
 * Vòng đời của refresh token theo từng phiên (bảng refresh_tokens).
 */
public interface IRefreshTokenService {

    /** Ghi nhận một phiên mới (gọi khi login). */
    RefreshToken create(String rawToken, User user, long validForSeconds, String userAgent);

    /** Trả về phiên tương ứng token, hoặc null nếu không hợp lệ/hết hạn/đã thu hồi. */
    RefreshToken verify(String rawToken);

    /** Xoay token: thu hồi token hiện tại và ghi nhận token mới. */
    RefreshToken rotate(RefreshToken current, String newRawToken, long validForSeconds, String userAgent);

    /** Thu hồi phiên gắn với token (gọi khi logout). */
    void revoke(String rawToken);

    /** Xoá các dòng đã hết hạn/đã thu hồi quá lâu; trả về số dòng đã xoá. */
    int purgeStale();
}
