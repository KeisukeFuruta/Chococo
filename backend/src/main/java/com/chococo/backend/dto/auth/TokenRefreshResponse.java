package com.chococo.backend.dto.auth;

// api-spec.md 3.10節：refreshのレスポンス形式（userは含まない）
public record TokenRefreshResponse(String token, String refreshToken) {
}
