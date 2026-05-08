package com.mopl.mopl.domain.notification.exception;

public class InvalidLimitValueException extends NotificationException {

  public InvalidLimitValueException() {
    super(NotificationErrorCode.INVALID_LIMIT_VALUE);
  }
}
