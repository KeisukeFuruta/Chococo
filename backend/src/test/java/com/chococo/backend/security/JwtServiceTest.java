package com.chococo.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

// auth-design.md 1章・2章：sub=ユーザーID、email claim、HS256署名のアクセストークンを扱うJwtServiceの単体テスト。
// Spring контекスト不要（secret/ttlを直接コンストラクタに渡せる設計にしているため高速に検証できる）
class JwtServiceTest {

    private static final String SECRET_BASE64 = Base64Secret.generate();

    private final JwtService jwtService = new JwtService(SECRET_BASE64, 60);

    @Test
    void generatedTokenParsesBackToTheSameUserIdAndEmail() {
        String token = jwtService.generateAccessToken(42L, "user@example.com");

        var parsed = jwtService.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(42L);
        assertThat(parsed.get().email()).isEqualTo("user@example.com");
    }

    @Test
    void tamperedTokenFailsToParse() {
        String token = jwtService.generateAccessToken(1L, "user@example.com");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtService.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    void malformedTokenFailsToParseWithoutThrowing() {
        assertThat(jwtService.parseAccessToken("not-a-jwt")).isEmpty();
    }

    @Test
    void expiredTokenFailsToParse() {
        // 署名鍵を共有した上で、有効期限が過去のトークンを直接組み立てる
        SecretKey key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(SECRET_BASE64));
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("email", "user@example.com")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThat(jwtService.parseAccessToken(expiredToken)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentKeyFailsToParse() {
        JwtService otherService = new JwtService(Base64Secret.generate(), 60);
        String tokenFromOtherService = otherService.generateAccessToken(1L, "user@example.com");

        assertThat(jwtService.parseAccessToken(tokenFromOtherService)).isEmpty();
    }

    private static final class Base64Secret {
        static String generate() {
            byte[] bytes = new byte[32];
            new java.security.SecureRandom().nextBytes(bytes);
            return java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }
}
