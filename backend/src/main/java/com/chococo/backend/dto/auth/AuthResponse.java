package com.chococo.backend.dto.auth;

// api-spec.md 3.1/3.2節：signup/loginのレスポンス形式
public record AuthResponse(String token, String refreshToken, UserDto user) {
}
