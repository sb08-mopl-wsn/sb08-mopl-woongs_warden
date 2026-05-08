package com.mopl.mopl.domain.notification.exception;

public class InvalidSortParameterException extends NotificationException {

  public InvalidSortParameterException() {
    super(NotificationErrorCode.INVALID_SORT_PARAMETER);
  }

  public InvalidSortParameterException(String messages) {
    super(NotificationErrorCode.INVALID_SORT_PARAMETER, messages);
  }
}
