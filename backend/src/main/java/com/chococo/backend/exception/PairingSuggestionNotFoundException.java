package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class PairingSuggestionNotFoundException extends ApiException {

    public PairingSuggestionNotFoundException() {
        // functional-spec.md 3.1節：存在しない場合と他人の提案である場合を区別しない
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "指定されたペアリング提案が見つかりません");
    }
}
