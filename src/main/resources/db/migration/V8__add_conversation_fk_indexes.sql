-- 대화방 검색 성능(COUNT, 페이징 조회)을 올리기 위한 non-unique 인덱스
CREATE INDEX IF NOT EXISTS idx_conversations_sender_id ON conversations (sender_id);
CREATE INDEX IF NOT EXISTS idx_conversations_receiver_id ON conversations (receiver_id);

-- DM 메시지 페이징 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_id ON direct_messages (conversation_id);