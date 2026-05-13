package com.mopl.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable ex) {

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        String sessionId = extractSessionId(clientMessage);
        String destination = extractDestination(clientMessage);

        // BusinessException 처리
        if (cause instanceof BusinessException exception) {
            log.warn("[WebSocket ERROR] sessionId = {}, destination={}, code={}, message={}",
                    sessionId,
                    destination,
                    exception.getErrorCode().getCode(),
                    exception.getMessage()
            );
            return errorMessage(exception.getMessage());
        }

        // BusinessException이 아닌 경우
        log.error("[WebSocket UNKNOWN ERROR] sessionId={}, destination={}, cause={}",
                sessionId,
                destination,
                ex.getMessage(),
                ex // 스택트레이스
        );

        return errorMessage("서버 내부 오류입니다.");
    }

    private String extractSessionId(Message<byte[]> message) {
        if (message == null) return "unknown";
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        return Optional.ofNullable(accessor.getSessionId()).orElse("unknown");
    }

    private String extractDestination(Message<byte[]> message) {
        if (message == null) return "unknown";
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        return Optional.ofNullable(accessor.getDestination()).orElse("unknown");
    }

    private Message<byte[]> errorMessage(String message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(message);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                message.getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}
