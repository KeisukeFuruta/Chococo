package com.chococo.backend.exception;

// api-spec.md 1.1節の共通エラーレスポンス形式: {"error": {"code": "...", "message": "..."}}
public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }
}
