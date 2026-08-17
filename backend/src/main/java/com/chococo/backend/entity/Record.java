package com.chococo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 1提案につき記録は最大1件（DBのUNIQUE制約）。使用済みチェックはexistsByPairingSuggestionIdで行う
    @Column(name = "pairing_suggestion_id", unique = true)
    private Long pairingSuggestionId;

    @Column(name = "sweet_name", nullable = false, length = 100)
    private String sweetName;

    // coffee_beansマスタ更新の影響を受けない、提案時点のスナップショット
    @Column(name = "coffee_bean_name", length = 100)
    private String coffeeBeanName;

    @Lob
    @Column(name = "ai_reason", length = 65535)
    private String aiReason;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Lob
    @Column(length = 65535)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
