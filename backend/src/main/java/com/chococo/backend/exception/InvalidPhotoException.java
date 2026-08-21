package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidPhotoException extends ApiException {

    public InvalidPhotoException() {
        // api-spec.md 3.6節・aws-infra-design.md 3.4節：写真はjpg/pngのみ許可
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "写真はjpgまたはpng形式のみアップロードできます");
    }
}
