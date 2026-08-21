package com.chococo.backend.dto.record;

import com.chococo.backend.entity.Record;
import java.time.LocalDate;

// api-spec.md 3.5節：カレンダー表示に必要な最小項目のみ
public record RecordListItemDto(Long id, String sweetName, LocalDate recordDate, String photoUrl, String coffeeBeanName) {

    public static RecordListItemDto from(Record record) {
        return new RecordListItemDto(
                record.getId(),
                record.getSweetName(),
                record.getRecordDate(),
                record.getPhotoPath(),
                record.getCoffeeBeanName());
    }
}
