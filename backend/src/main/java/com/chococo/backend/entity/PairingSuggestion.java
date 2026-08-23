package com.chococo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "pairing_suggestions")
public class PairingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sweet_name", nullable = false, length = 100)
    private String sweetName;

    // coffee_beansは10件固定のマスタテーブルのため、素のIDではなくエンティティ参照にして
    // レスポンス組み立て時に追加クエリなしで名称・焙煎度等を取得できるようにする
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_bean_id", nullable = false)
    private CoffeeBean coffeeBean;

    @Lob
    @Column(nullable = false, length = 65535)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PairingSuggestion() {
    }

    public PairingSuggestion(
            Long id, Long userId, String sweetName, CoffeeBean coffeeBean, String reason, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.sweetName = sweetName;
        this.coffeeBean = coffeeBean;
        this.reason = reason;
        this.createdAt = createdAt;
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

    public String getSweetName() {
        return sweetName;
    }

    public void setSweetName(String sweetName) {
        this.sweetName = sweetName;
    }

    public CoffeeBean getCoffeeBean() {
        return coffeeBean;
    }

    public void setCoffeeBean(CoffeeBean coffeeBean) {
        this.coffeeBean = coffeeBean;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String sweetName;
        private CoffeeBean coffeeBean;
        private String reason;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder sweetName(String sweetName) {
            this.sweetName = sweetName;
            return this;
        }

        public Builder coffeeBean(CoffeeBean coffeeBean) {
            this.coffeeBean = coffeeBean;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PairingSuggestion build() {
            return new PairingSuggestion(id, userId, sweetName, coffeeBean, reason, createdAt);
        }
    }
}
