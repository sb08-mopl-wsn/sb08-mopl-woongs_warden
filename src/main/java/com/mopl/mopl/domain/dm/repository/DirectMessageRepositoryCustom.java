package com.mopl.mopl.domain.dm.repository;

import com.mopl.mopl.domain.dm.entity.DirectMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {

  // 가장 최근 메시지 1개 조회 (미리보기용)
  DirectMessage findLatestMessage(UUID conversationId);

  // 커서 기반 페이징 조회
  List<DirectMessage> findMessagesByCursor(UUID conversationId, Instant cursor, UUID idAfter, int limit, String sortDirection);
}
