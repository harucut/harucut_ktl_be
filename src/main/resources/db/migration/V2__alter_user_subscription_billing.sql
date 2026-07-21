-- user_subscription 에 결제 기반 구독 상태/기간 컬럼 추가.
-- 기존 행은 전부 무료(BASIC) 구독이므로 DEFAULT 로 무중단 backfill.
ALTER TABLE user_subscription
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER plan_tier,
    ADD COLUMN current_period_start DATETIME(6) NULL AFTER status,
    ADD COLUMN current_period_end DATETIME(6) NULL AFTER current_period_start,
    ADD COLUMN auto_renew BOOLEAN NOT NULL DEFAULT FALSE AFTER current_period_end,
    ADD COLUMN billing_key_id BIGINT NULL AFTER auto_renew;
