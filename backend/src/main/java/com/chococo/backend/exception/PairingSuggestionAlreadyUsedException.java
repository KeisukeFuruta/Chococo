package com.chococo.backend.exception;

import org.springframework.http.HttpStatus;

public class PairingSuggestionAlreadyUsedException extends ApiException {

    public PairingSuggestionAlreadyUsedException() {
        super(
                HttpStatus.CONFLICT,
                "PAIRING_SUGGESTION_ALREADY_USED",
                "このペアリング提案は既に別の記録で使用されています");
    }
}
