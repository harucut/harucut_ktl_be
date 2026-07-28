CREATE TABLE coupon (
    coupon_id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(12) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(32) NOT NULL,
    grant_tier VARCHAR(16) NOT NULL,
    max_redemptions INT NULL,
    valid_until DATETIME(6) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (coupon_id),
    UNIQUE KEY uk_coupon_public_id (public_id),
    UNIQUE KEY uk_coupon_code (code)
) ENGINE = InnoDB;

CREATE TABLE user_coupon (
    user_coupon_id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(12) NOT NULL,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    redeemed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_coupon_id),
    UNIQUE KEY uk_user_coupon_public_id (public_id),
    UNIQUE KEY uk_user_coupon_user_coupon (user_id, coupon_id),
    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id)
) ENGINE = InnoDB;

CREATE INDEX idx_user_coupon_user_id ON user_coupon (user_id);
