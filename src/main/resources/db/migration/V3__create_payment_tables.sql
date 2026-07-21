CREATE TABLE billing_key (
    billing_key_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(16) NOT NULL,
    billing_key_value VARCHAR(255) NOT NULL,
    masked_card VARCHAR(32) NULL,
    public_id VARCHAR(12) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (billing_key_id),
    UNIQUE KEY uk_billing_key_public_id (public_id),
    CONSTRAINT fk_billing_key_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE INDEX idx_billing_key_user_id ON billing_key (user_id);

CREATE TABLE payment_order (
    payment_order_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_tier VARCHAR(16) NOT NULL,
    amount INT NOT NULL,
    order_type VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    public_id VARCHAR(12) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (payment_order_id),
    UNIQUE KEY uk_payment_order_idempotency_key (idempotency_key),
    UNIQUE KEY uk_payment_order_public_id (public_id),
    CONSTRAINT fk_payment_order_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE INDEX idx_payment_order_user_id ON payment_order (user_id);

CREATE TABLE payment (
    payment_id BIGINT NOT NULL AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL,
    amount INT NOT NULL,
    method VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    pg_transaction_id VARCHAR(100) NULL,
    failure_code VARCHAR(50) NULL,
    failure_message VARCHAR(255) NULL,
    approved_at DATETIME(6) NULL,
    public_id VARCHAR(12) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_public_id (public_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (payment_order_id) REFERENCES payment_order (payment_order_id)
) ENGINE = InnoDB;

CREATE INDEX idx_payment_order_id ON payment (payment_order_id);

ALTER TABLE user_subscription
    ADD CONSTRAINT fk_user_subscription_billing_key FOREIGN KEY (billing_key_id) REFERENCES billing_key (billing_key_id);
