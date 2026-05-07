package com.mopl.mopl.domain.notification.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND(4001, "NOT_FOUND", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  NOTIFICATION_ACCESS_DENIED(4002, "ACCESS_DENIED", HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() { return "NOTI"; }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
