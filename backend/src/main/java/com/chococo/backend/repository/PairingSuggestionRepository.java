package com.chococo.backend.repository;

import com.chococo.backend.entity.PairingSuggestion;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingSuggestionRepository extends JpaRepository<PairingSuggestion, Long> {

    // functional-spec.md 3.2節：1日10回制限（JST基準）のカウントに使用。
    // (user_id, created_at)の複合インデックス（database-design.md 2.3節）を使う想定のクエリ
    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant createdAtFrom);
}
