package com.mopl.mopl.domain.notification.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class NotificationException extends BusinessException {

  public NotificationException(NotificationErrorCode errorCode) {
    super(errorCode);
  }

  public NotificationException(NotificationErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
