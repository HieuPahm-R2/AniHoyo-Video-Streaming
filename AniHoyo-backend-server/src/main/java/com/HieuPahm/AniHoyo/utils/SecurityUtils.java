package com.HieuPahm.AniHoyo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.HieuPahm.AniHoyo.model.dtos.auth.ResLoginDTO;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class SecurityUtils {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;
    private final JwtEncoder jwtEncoder;

    public SecurityUtils(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Value("${anihoyo.jwt.base64-secret}")
    private String jwtKey;

    @Value("${anihoyo.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpire;
    @Value("${anihoyo.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpire;

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    public SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    public Jwt confirmValidRefreshToken(String token) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey()).macAlgorithm(JWT_ALGORITHM)
                .build();
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            System.out.println(">>> Refresh_token gets error: " + e.getMessage());
            throw e;
        }
    }

    public String generateAccessToken(String email, ResLoginDTO user) {
        ResLoginDTO.InfoWithinToken token = new ResLoginDTO.InfoWithinToken();
        token.setId(user.getUser().getId());
        token.setEmail(user.getUser().getEmail());
        token.setName(user.getUser().getName());
        // assign to claim
        Instant now = Instant.now();
        Instant validity = now.plus(this.accessTokenExpire, ChronoUnit.SECONDS);

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuedAt(now)
        .expiresAt(validity)
        .subject(email)
        .claim("user account", token)
        // jti: hai lần phát hành trong cùng một giây vẫn ra token khác nhau —
        // bắt buộc để token_hash trong bảng refresh_tokens là duy nhất.
        .id(UUID.randomUUID().toString())
        .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
    
    public String generateRefreshToken(String emailLogin, ResLoginDTO resLoginDTO){
        ResLoginDTO.InfoWithinToken data = new ResLoginDTO.InfoWithinToken();
        data.setId(resLoginDTO.getUser().getId());
        data.setEmail(resLoginDTO.getUser().getEmail());
        data.setName(resLoginDTO.getUser().getName());
         // assign to claim
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpire, ChronoUnit.SECONDS);
        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuedAt(now)
        .expiresAt(validity)
        .subject(emailLogin)
        .claim("user account", data)
        // jti: hai lần login trong cùng một giây vẫn ra token khác nhau —
        // bắt buộc để token_hash trong bảng refresh_tokens là duy nhất.
        .id(UUID.randomUUID().toString())
        .build();
        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * SHA-256 hex của một refresh token.
     *
     * Bảng refresh_tokens chỉ lưu hash nên nếu dữ liệu bị lộ thì cũng không dùng
     * lại được token; tra cứu vẫn là một phép so khớp trên index unique.
     */
    public static String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM này", exception);
        }
    }
}
