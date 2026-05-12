-- 기존의 불완전한 유니크 제약조건 삭제
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS uk_conversations_sender_receiver;
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS conversations_sender_id_receiver_id_key;

-- 새 컬럼 추가
ALTER TABLE conversations ADD COLUMN participant_pair_key VARCHAR(100);

-- 새 컬럼에 NOT NULL 조건, 유니크 제약조건 추가
ALTER TABLE conversations ALTER COLUMN participant_pair_key SET NOT NULL;
ALTER TABLE conversations ADD CONSTRAINT uk_conversations_participant_pair UNIQUE (participant_pair_key);