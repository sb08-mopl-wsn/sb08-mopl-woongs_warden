package com.mopl.mopl.domain.notification.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND(4001, "NOT_FOUND", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  NOTIFICATION_ACCESS_DENIED(4002, "ACCESS_DENIED", HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),
  INVALID_CURSOR_FORMAT(4003, "INVALID_CURSOR", HttpStatus.BAD_REQUEST, "잘못된 형식의 커서 데이터입니다."),
  INVALID_LIMIT_VALUE(4004, "INVALID_LIMIT", HttpStatus.BAD_REQUEST, "limit 값은 1 이상이어야 합니다."),
  INVALID_SORT_PARAMETER(4005, "INVALID_SORT", HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 조건입니다.");

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
