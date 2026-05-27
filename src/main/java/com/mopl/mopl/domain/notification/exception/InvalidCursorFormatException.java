package com.mopl.mopl.domain.notification.exception;

public class InvalidCursorFormatException extends NotificationException{

  public InvalidCursorFormatException() {
    super(NotificationErrorCode.INVALID_CURSOR_FORMAT);
  }
}
