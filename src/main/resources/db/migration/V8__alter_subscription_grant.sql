-- user_subscription 에 현 주기 후 활성화 대기 중인 무료 grant 쿠폰(UserCoupon) id 컬럼 추가.
-- 기존 행은 예약이 없으므로 NULL 로 무중단 backfill. FK 는 걸지 않는다(배치 조회 전용).
ALTER TABLE user_subscription
    ADD COLUMN reserved_grant_coupon_id BIGINT NULL AFTER billing_key_id;
