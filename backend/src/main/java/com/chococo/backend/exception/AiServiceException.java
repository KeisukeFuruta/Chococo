package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends ApiException {

    public AiServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR", message);
    }

    public AiServiceException(String message, Throwable cause) {
        this(message);
        initCause(cause);
    }
}
