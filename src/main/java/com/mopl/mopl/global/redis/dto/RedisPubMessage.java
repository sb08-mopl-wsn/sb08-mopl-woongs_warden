package com.mopl.mopl.global.redis.dto;

import java.util.UUID;

/**
 * Redis 토픽으로 주고받을 메시지 형식
 * @param targetUserId 이 메시지를 받아야 할 최종 유저 ID
 * @param eventName SSE의 eventName 또는 WebSocket의 destination
 * @param data 실제 전송될 데이터 (직렬화 가능)
 */
public record RedisPubMessage(
    UUID targetUserId,
    String eventName,
    Object data
) {

}
