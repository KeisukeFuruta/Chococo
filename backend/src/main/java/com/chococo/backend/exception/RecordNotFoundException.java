package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class RecordNotFoundException extends ApiException {

    public RecordNotFoundException() {
        // auth-design.md 10節：存在しない場合と他人の記録である場合を区別しない
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "指定された記録が見つかりません");
    }
}
