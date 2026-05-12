package com.mopl.mopl.domain.conversation.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConversationErrorCode implements ErrorCode {

  CONVERSATION_NOT_FOUND(6501, "NOT_FOUND", HttpStatus.NOT_FOUND, "대화방을 찾을 수 없습니다."),
  CONVERSATION_ACCESS_DENIED(6502, "ACCESS_DENIED", HttpStatus.FORBIDDEN, "해당 대화방에 접근할 권한이 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() { return "CONV"; }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
