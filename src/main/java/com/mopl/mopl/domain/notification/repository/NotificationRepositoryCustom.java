package com.mopl.mopl.domain.notification.repository;

import com.mopl.mopl.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

  List<Notification> findNotificationsByCursor(
      UUID userId, boolean isAsc, Instant cursor, UUID idAfter, Pageable pageable
  );
}
