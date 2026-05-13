package com.mopl.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

// GlobalExceptionHandler는 @RestControllerAdvice로 HTTP 전용이며
// WebSocketExceptionHandler는 @ControllerAdvice + @MessageExceptionHandler로 WebSocket 전용입니다.
@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(BusinessException.class)
    public void handleWebsocketException(BusinessException e) {
        ErrorCode code = e.getErrorCode();

        log.warn("[WebSocket FAIL] code={}, message={}",
                code.getCode(), e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e) {
        log.error("[WebSocket UNKNOWN ERROR] message={}", e.getMessage(), e);
    }
}
