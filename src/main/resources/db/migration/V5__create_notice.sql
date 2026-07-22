CREATE TABLE notice (
    notice_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    pinned BOOLEAN NOT NULL,
    public_id VARCHAR(12) NOT NULL,
    published BOOLEAN NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (notice_id),
    UNIQUE KEY uk_notice_public_id (public_id)
) ENGINE = InnoDB;

CREATE INDEX idx_notice_published_pinned ON notice (published, pinned);
