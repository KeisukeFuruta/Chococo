package com.chococo.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

// auth-design.md 4-2節：クライアントに渡す生の値はDBに保存せず、SHA-256ハッシュのみを保存する
// （パスワードをBCryptでハッシュ化するのと同じ考え方）
@Component
public class RefreshTokenHasher {

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256は全JVMで標準サポートされているため実質到達しない
            throw new IllegalStateException(e);
        }
    }
}
