package com.mopl.mopl.domain.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends NotificationException{

  public NotificationNotFoundException(UUID notificationId) {
    super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
  }
}
