package com.chococo.backend.dto.pairing;

// api-spec.md 3.3節のレスポンス形式。resetAtは"2026-08-16T00:00:00+09:00"形式の文字列（JST固定）
public record PairingUsageResponse(long usedCount, int limit, long remainingCount, String resetAt) {
}
