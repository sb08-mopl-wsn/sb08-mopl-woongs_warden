-- 1:1 대화방의 두 참여자(Sender, Receiver)가 각각 마지막으로 읽은 메시지의 시간을 기록하는 컬럼 추가
ALTER TABLE conversations
    ADD COLUMN last_read_at_by_sender TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_read_at_by_receiver TIMESTAMP WITH TIME ZONE;