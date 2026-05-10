package com.mopl.mopl.global.exception;

public class SseConnectionException extends BusinessException {

  public SseConnectionException() {
    super(GlobalErrorCode.SSE_CONNECTION_FAILED);
  }

  public SseConnectionException(Throwable cause) {
    super(GlobalErrorCode.SSE_CONNECTION_FAILED, "SSE 연결 초기화 실패: " + cause.getMessage());
  }
}
