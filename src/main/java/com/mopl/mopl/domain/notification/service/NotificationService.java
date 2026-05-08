package com.mopl.mopl.domain.notification.service;

import com.mopl.mopl.domain.notification.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import java.util.UUID;

public interface NotificationService {

  // 알림 삭제 (읽음 처리)
  void deleteNotification(UUID userId, UUID notificationId);

  // 알림 목록 조회
  CursorResponseNotificationDto getNotifications(UUID userId, CursorPaginationRequest request);
}
