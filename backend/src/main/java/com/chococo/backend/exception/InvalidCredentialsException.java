package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        // auth-design.md 8節：メール・パスワードのどちらが誤りかは特定させない固定文言
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "メールアドレスまたはパスワードが正しくありません");
    }
}
