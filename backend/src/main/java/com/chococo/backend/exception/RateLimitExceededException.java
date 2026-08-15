package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException() {
        // functional-spec.md 3.2節：1ユーザー1日10回まで（JST基準）
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "本日のAI利用上限に達しました");
    }
}
