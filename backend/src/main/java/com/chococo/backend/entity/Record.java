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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "records")
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

    public Record() {
    }

    public Record(
            Long id,
            Long userId,
            Long pairingSuggestionId,
            String sweetName,
            String coffeeBeanName,
            String aiReason,
            String photoPath,
            LocalDate recordDate,
            String comment,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.pairingSuggestionId = pairingSuggestionId;
        this.sweetName = sweetName;
        this.coffeeBeanName = coffeeBeanName;
        this.aiReason = aiReason;
        this.photoPath = photoPath;
        this.recordDate = recordDate;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPairingSuggestionId() {
        return pairingSuggestionId;
    }

    public void setPairingSuggestionId(Long pairingSuggestionId) {
        this.pairingSuggestionId = pairingSuggestionId;
    }

    public String getSweetName() {
        return sweetName;
    }

    public void setSweetName(String sweetName) {
        this.sweetName = sweetName;
    }

    public String getCoffeeBeanName() {
        return coffeeBeanName;
    }

    public void setCoffeeBeanName(String coffeeBeanName) {
        this.coffeeBeanName = coffeeBeanName;
    }

    public String getAiReason() {
        return aiReason;
    }

    public void setAiReason(String aiReason) {
        this.aiReason = aiReason;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private Long pairingSuggestionId;
        private String sweetName;
        private String coffeeBeanName;
        private String aiReason;
        private String photoPath;
        private LocalDate recordDate;
        private String comment;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder pairingSuggestionId(Long pairingSuggestionId) {
            this.pairingSuggestionId = pairingSuggestionId;
            return this;
        }

        public Builder sweetName(String sweetName) {
            this.sweetName = sweetName;
            return this;
        }

        public Builder coffeeBeanName(String coffeeBeanName) {
            this.coffeeBeanName = coffeeBeanName;
            return this;
        }

        public Builder aiReason(String aiReason) {
            this.aiReason = aiReason;
            return this;
        }

        public Builder photoPath(String photoPath) {
            this.photoPath = photoPath;
            return this;
        }

        public Builder recordDate(LocalDate recordDate) {
            this.recordDate = recordDate;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Record build() {
            return new Record(
                    id,
                    userId,
                    pairingSuggestionId,
                    sweetName,
                    coffeeBeanName,
                    aiReason,
                    photoPath,
                    recordDate,
                    comment,
                    createdAt,
                    updatedAt);
        }
    }
}
