package com.mopl.mopl.domain.dm.repository;

import com.mopl.mopl.domain.dm.entity.DirectMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageRepositoryCustom {

  // 이 방에 있는 전체 메시지 수 (첫 로딩 시 최적화용)
  long countByConversationId(UUID conversationId);
}