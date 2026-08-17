package com.chococo.backend.repository;

import com.chococo.backend.entity.PairingSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingSuggestionRepository extends JpaRepository<PairingSuggestion, Long> {
}
