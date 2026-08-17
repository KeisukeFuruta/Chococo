package com.chococo.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// auth-design.md 1章・2章：sub=ユーザーID、email カスタムclaim、HS256署名のアクセストークン(JWT)を扱う。
// リフレッシュトークン（不透明な乱数文字列）はここでは扱わない（RefreshTokenServiceの責務）
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${chococo.jwt.secret}") String secret,
            @Value("${chococo.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        // auth-design.md 2章：Base64エンコードされた256bit鍵から生成する運用だが、
        // ローカル開発用フォールバック値は非Base64の平文プレースホルダーのため、
        // UTF-8バイト列としてもそのまま鍵材料に使えるようにする
        this.key = Keys.hmacShaKeyFor(decodeSecret(secret));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
    }

    private static byte[] decodeSecret(String secret) {
        try {
            return java.util.Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    // auth-design.md 5節：検証失敗時（期限切れ・改ざん・不正形式）は例外を投げず空のOptionalを返す。
    // 呼び出し側（JwtAuthenticationFilter）はこれを受けてSecurityContextを未設定のまま次へ進める
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            return Optional.of(new AuthenticatedUser(userId, email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
