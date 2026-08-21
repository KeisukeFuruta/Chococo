package com.chococo.backend.dto.pairing;

// api-spec.md 3.4節のレスポンス形式
public record PairingSuggestionResponse(
        Long pairingSuggestionId,
        String sweetName,
        CoffeeBeanDto coffeeBean,
        String reason,
        long remainingCount) {
}
