package com.mopl.mopl.domain.conversation.repository;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ConversationRepositoryCustom {

  List<Conversation> findMyConversationsByCursor(
      UUID userId, String keywordLike, String sortBy, boolean isAsc, Instant cursor, UUID idAfter, Pageable pageable
  );

  long countMyConversationsByCursorCondition(UUID userId, String keywordLike);
}
