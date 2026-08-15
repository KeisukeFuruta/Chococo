CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE coffee_beans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    roast_level VARCHAR(20) NOT NULL,
    origin VARCHAR(50) NULL,
    description TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pairing_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sweet_name VARCHAR(100) NOT NULL,
    coffee_bean_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_pairing_suggestions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_pairing_suggestions_coffee_bean FOREIGN KEY (coffee_bean_id) REFERENCES coffee_beans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_pairing_suggestions_user_created ON pairing_suggestions(user_id, created_at);

CREATE TABLE records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pairing_suggestion_id BIGINT NULL,
    sweet_name VARCHAR(100) NOT NULL,
    coffee_bean_name VARCHAR(100) NULL,
    ai_reason TEXT NULL,
    photo_path VARCHAR(500) NULL,
    record_date DATE NOT NULL,
    comment TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_records_pairing_suggestion_id UNIQUE (pairing_suggestion_id),
    CONSTRAINT fk_records_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_records_pairing_suggestion FOREIGN KEY (pairing_suggestion_id) REFERENCES pairing_suggestions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_records_user_date ON records(user_id, record_date);
