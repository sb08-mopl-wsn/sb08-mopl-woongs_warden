package com.mopl.mopl.domain.conversation.repository;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  // 두 유저 간의 대화방이 이미 존재하는 지 찾기 (발신/수신 방향 무관하게 하나라도 있으면 같은 방)
  @Query("SELECT c FROM Conversation c WHERE " +
      "(c.sender.id = :userId1 AND c.receiver.id = :userId2) OR " +
      "(c.sender.id = :userId2 AND c.receiver.id = :userId1)")
  Optional<Conversation> findConversationBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

  // 내가 참여 중인 모든 대화방 목록 조회 (최근 업데이트된 방 순서대로 정렬 - DESCENDING)
  @Query("SELECT c FROM Conversation c " +
        "WHERE (c.sender.id = :userId OR c.receiver.id = :userId) " +
      "AND (CAST(:cursor AS timestamp) IS NULL OR " +
      "    c.updatedAt < :cursor OR " +
      "    (c.updatedAt = :cursor AND (:idAfter IS NULL OR c.id < :idAfter))) " +
      "ORDER BY c.updatedAt DESC, c.id DESC")
  List<Conversation> findMyConversationsByCursorDesc(
      @Param("userId") UUID userId,
      @Param("cursor")Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  // 대화방 목록 오름차순 정렬 - ASCENDING
  @Query("SELECT c FROM Conversation c " +
      "WHERE (c.sender.id = :userId OR c.receiver.id = :userId) " +
      "AND (CAST(:cursor AS timestamp) IS NULL OR " +
      "     c.updatedAt > :cursor OR " +
      "     (c.updatedAt = :cursor AND (:idAfter IS NULL OR c.id > :idAfter))) " +
      "ORDER BY c.updatedAt ASC, c.id ASC")
  List<Conversation> findMyConversationsByCursorAsc(
      @Param("userId") UUID userId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );

  // 내가 참여 중인 대화방의 총 개수
  @Query("SELECT COUNT(c) FROM Conversation c WHERE c.sender.id = :userId OR c.receiver.id = :userId")
  long countMyConversations(@Param("userId") UUID userId);
}
