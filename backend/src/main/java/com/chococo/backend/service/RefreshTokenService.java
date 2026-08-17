package com.chococo.backend.service;

import com.chococo.backend.entity.RefreshToken;
import com.chococo.backend.exception.RefreshTokenInvalidException;
import com.chococo.backend.repository.RefreshTokenRepository;
import com.chococo.backend.security.RefreshTokenHasher;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// auth-design.md 4-2節：不透明な乱数文字列（32バイト、Base64URL）を発行し、DBにはSHA-256ハッシュのみ保存する。
// 使用のたびに旧レコードを削除して新規発行する「ローテーション」方式
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher hasher;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenHasher hasher,
            @Value("${chococo.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.hasher = hasher;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    @Transactional
    public String issue(Long userId) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hasher.hash(rawToken))
                .expiresAt(Instant.now().plus(refreshTokenTtl))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    // 検証 → 該当レコード削除 → 新規発行までを1トランザクションで行う（auth-design.md 4-2節のローテーション）。
    // 戻り値は新しいリフレッシュトークンの生の値と、対象ユーザーID
    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hasher.hash(rawToken))
                .orElseThrow(RefreshTokenInvalidException::new);

        refreshTokenRepository.delete(existing);

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenInvalidException();
        }

        String newRawToken = issue(existing.getUserId());
        return new RotationResult(newRawToken, existing.getUserId());
    }

    // ログアウトはトークンの成否によらず204を返す仕様（api-spec.md 3.11節）のため、存在しなくても例外は投げない
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(hasher.hash(rawToken));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(String rawToken, Long userId) {
    }
}
