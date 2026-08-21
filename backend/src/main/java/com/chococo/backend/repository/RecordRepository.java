package com.chococo.backend.repository;

import com.chococo.backend.entity.Record;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<Record, Long> {

    // database-design.md 2.4節：(user_id, record_date)の複合インデックスを使うカレンダー月次一覧取得
    List<Record> findByUserIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
            Long userId, LocalDate recordDateFrom, LocalDate recordDateTo);

    // Record.javaのコメント参照：UNIQUE制約(pairing_suggestion_id)に対応する使用済みチェック
    boolean existsByPairingSuggestionId(Long pairingSuggestionId);

    // functional-spec.md 3.5節：詳細・編集・削除は全操作で所有権チェック（WHERE id=? AND user_id=?）を徹底する
    Optional<Record> findByIdAndUserId(Long id, Long userId);
}
