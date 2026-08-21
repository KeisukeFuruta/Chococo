package com.chococo.backend.dto.record;

import com.chococo.backend.entity.Record;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// api-spec.md 3.6節のレスポンス形式
public record RecordResponse(
        Long id,
        String sweetName,
        LocalDate recordDate,
        String comment,
        String photoUrl,
        String coffeeBeanName,
        String aiReason,
        String createdAt,
        String updatedAt) {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    public static RecordResponse from(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getSweetName(),
                record.getRecordDate(),
                record.getComment(),
                record.getPhotoPath(),
                record.getCoffeeBeanName(),
                record.getAiReason(),
                format(record.getCreatedAt()),
                format(record.getUpdatedAt()));
    }

    private static String format(Instant instant) {
        return instant.atZone(JST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
