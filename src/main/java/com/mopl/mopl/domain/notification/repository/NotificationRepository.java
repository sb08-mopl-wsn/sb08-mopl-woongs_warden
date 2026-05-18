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

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

  // 본인의 알림인지 확인 후 조회/삭제
  Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

  // 사용자의 총 알림 개수
  long countByUserId(UUID userId);
}
