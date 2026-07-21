-- Baseline: 현재(Flyway 도입 시점) 운영 스키마 전체.
-- staging/prod는 이미 존재하는 스키마이므로 baseline-on-migrate 로 "적용된 것으로 표시"만 되고 실제 실행되지 않는다.
-- local/test는 spring.flyway.enabled=false 이므로 이 파일은 실행되지 않고 Hibernate ddl-auto=create-drop 이 대신 스키마를 만든다.

CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(64) NULL,
    user_role VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NULL,
    username VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(1024) NOT NULL,
    user_status VARCHAR(255) NOT NULL,
    delete_requested_at DATETIME(6) NULL,
    public_id VARCHAR(12) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_public_id (public_id)
) ENGINE = InnoDB;

CREATE TABLE user_subscription (
    subscription_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_tier VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (subscription_id),
    UNIQUE KEY uk_user_subscription_user_id (user_id),
    CONSTRAINT fk_user_subscription_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE TABLE terms (
    terms_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    required BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (terms_id),
    UNIQUE KEY uk_terms_code (code)
) ENGINE = InnoDB;

CREATE TABLE terms_version (
    terms_version_id BIGINT NOT NULL AUTO_INCREMENT,
    terms_id BIGINT NOT NULL,
    version INT NOT NULL,
    content LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (terms_version_id),
    UNIQUE KEY uk_terms_version_terms_id_version (terms_id, version),
    CONSTRAINT fk_terms_version_terms FOREIGN KEY (terms_id) REFERENCES terms (terms_id)
) ENGINE = InnoDB;

CREATE TABLE terms_consent (
    terms_consent_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    terms_version_id BIGINT NOT NULL,
    agreed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (terms_consent_id),
    CONSTRAINT fk_terms_consent_terms_version FOREIGN KEY (terms_version_id) REFERENCES terms_version (terms_version_id)
) ENGINE = InnoDB;

CREATE INDEX idx_terms_consent_user_id ON terms_consent (user_id);

CREATE TABLE frame (
    frame_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    preview_key VARCHAR(1024) NOT NULL,
    frame_type VARCHAR(32) NOT NULL,
    background JSON NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (frame_id),
    CONSTRAINT fk_frame_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

CREATE TABLE frame_component (
    frame_component_id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(1024) NOT NULL,
    type VARCHAR(32) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    width DOUBLE NULL,
    height DOUBLE NULL,
    scale DOUBLE NULL,
    rotation DOUBLE NOT NULL,
    z_index INT NOT NULL,
    style_json JSON NULL,
    frame_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (frame_component_id),
    CONSTRAINT fk_frame_component_frame FOREIGN KEY (frame_id) REFERENCES frame (frame_id)
) ENGINE = InnoDB;

CREATE TABLE user_media (
    media_id BIGINT NOT NULL AUTO_INCREMENT,
    media_type VARCHAR(32) NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (media_id),
    CONSTRAINT fk_user_media_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB;
