package com.chococo.backend.dto.record;

import java.util.List;

// api-spec.md 3.5節のレスポンス形式
public record RecordListResponse(List<RecordListItemDto> records) {
}
