package com.chococo.backend.service;

import com.chococo.backend.dto.record.RecordListItemDto;
import com.chococo.backend.dto.record.RecordListResponse;
import com.chococo.backend.dto.record.RecordResponse;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.entity.Record;
import com.chococo.backend.exception.PairingSuggestionAlreadyUsedException;
import com.chococo.backend.exception.PairingSuggestionNotFoundException;
import com.chococo.backend.repository.PairingSuggestionRepository;
import com.chococo.backend.repository.RecordRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// api-spec.md 3.5/3.6節、functional-spec.md 3.3/3.4節
@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final PairingSuggestionRepository pairingSuggestionRepository;
    private final PhotoStorageService photoStorageService;

    public RecordService(
            RecordRepository recordRepository,
            PairingSuggestionRepository pairingSuggestionRepository,
            PhotoStorageService photoStorageService) {
        this.recordRepository = recordRepository;
        this.pairingSuggestionRepository = pairingSuggestionRepository;
        this.photoStorageService = photoStorageService;
    }

    @Transactional(readOnly = true)
    public RecordListResponse listByMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<Record> records = recordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return new RecordListResponse(records.stream().map(RecordListItemDto::from).toList());
    }

    @Transactional
    public RecordResponse create(
            Long userId,
            String sweetName,
            LocalDate recordDate,
            String comment,
            Long pairingSuggestionId,
            MultipartFile photo) {
        String coffeeBeanName = null;
        String aiReason = null;
        if (pairingSuggestionId != null) {
            PairingSuggestion suggestion = pairingSuggestionRepository
                    .findById(pairingSuggestionId)
                    .filter(s -> s.getUserId().equals(userId))
                    // functional-spec.md 3.1節と同様、存在しない場合と他人の提案である場合を区別しない
                    .orElseThrow(PairingSuggestionNotFoundException::new);
            // database-design.md 2.4節：UNIQUE制約(pairing_suggestion_id)により1提案につき記録は最大1件
            if (recordRepository.existsByPairingSuggestionId(pairingSuggestionId)) {
                throw new PairingSuggestionAlreadyUsedException();
            }
            coffeeBeanName = suggestion.getCoffeeBean().getName();
            aiReason = suggestion.getReason();
        }

        String photoPath = (photo == null || photo.isEmpty()) ? null : photoStorageService.store(photo);

        Record record = Record.builder()
                .userId(userId)
                .pairingSuggestionId(pairingSuggestionId)
                .sweetName(sweetName)
                .coffeeBeanName(coffeeBeanName)
                .aiReason(aiReason)
                .photoPath(photoPath)
                .recordDate(recordDate)
                .comment(comment)
                .build();
        recordRepository.save(record);

        return RecordResponse.from(record);
    }
}
