package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenInvalidException extends ApiException {

    public RefreshTokenInvalidException() {
        super(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "リフレッシュトークンが無効です");
    }
}
