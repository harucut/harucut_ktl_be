-- 기본 제공(시스템) 프레임 지원: user_id nullable 전환 + is_system 플래그 추가.
-- 기존 행은 전부 사용자 소유 프레임이므로 DEFAULT FALSE 로 무중단 backfill.
ALTER TABLE frame
    MODIFY COLUMN user_id BIGINT NULL,
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE AFTER user_id;

-- 불변식(is_system=true ↔ user_id=null)을 DB 레벨에서도 강제.
-- backfill이 끝난 뒤(모든 기존 행이 is_system=false/user_id NOT NULL)에 걸어야 위반 없이 통과한다.
ALTER TABLE frame ADD CONSTRAINT ck_frame_system
    CHECK ((is_system = 1 AND user_id IS NULL) OR (is_system = 0 AND user_id IS NOT NULL));
