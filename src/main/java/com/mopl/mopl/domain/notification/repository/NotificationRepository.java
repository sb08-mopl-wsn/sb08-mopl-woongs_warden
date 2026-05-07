package com.mopl.mopl.domain.notification.repository;

import com.mopl.mopl.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // 본인의 알림인지 확인 후 조회/삭제
  Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

  // 사용자의 총 알림 개수
  long countByUserId(UUID userId);

  // 커서 기반 알림 목록 조회 내림차순(최신순)
  @Query("SELECT n FROM Notification n " +
      "WHERE n.user.id = :userId " +
      "AND (CAST(:cursor AS timestamp) IS NULL OR " +
      "n.createdAt < :cursor OR " +
      "(n.createdAt = :cursor AND n.id < :idAfter))" +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findNotificationsByCursorDesc(
    @Param("userId") UUID userId,
    @Param("cursor")Instant cursor,
    @Param("idAfter") UUID idAfter,
    Pageable pageable
  );

  // 커서 기반 알림 목록 조회 오름차순(오래된 순)
  @Query("SELECT n FROM Notification n " +
      "WHERE n.user.id = :userId " +
      "AND (CAST(:cursor AS timestamp) IS NULL OR " +
      "n.createdAt > :cursor OR " +
      "(n.createdAt = :cursor AND n.id > :idAfter))" +
      "ORDER BY n.createdAt ASC, n.id ASC")
  List<Notification> findNotificationsByCursorAsc(
      @Param("userId") UUID userId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable
  );
}
