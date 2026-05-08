package com.mopl.mopl.domain.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends NotificationException{

  public NotificationNotFoundException(UUID notificationId) {
    super(NotificationErrorCode.NOTIFICATION_NOT_FOUND, "알림을 찾을 수 없습니다. 알림 ID" + notificationId);
  }
}
