package com.mopl.mopl.domain.conversation.repository;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>, ConversationRepositoryCustom {

  // 두 유저 간의 대화방이 이미 존재하는 지 찾기 (발신/수신 방향 무관하게 하나라도 있으면 같은 방)
  Optional<Conversation> findByParticipantPairKey(String participantPairKey);

    // 내가 참여 중인 대화방의 총 개수
  long countBySenderId(UUID senderId);
  long countByReceiverId(UUID receiverId);
}
