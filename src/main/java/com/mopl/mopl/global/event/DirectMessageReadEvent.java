package com.mopl.mopl.global.event;

import java.time.Instant;
import java.util.UUID;

/**
 * DM 읽은 사람, 시간 정보를 담은 이벤트
 * @param conversationId 대화방 id
 * @param readerId 읽은 사람 id
 * @param readAt 몇 시에 쓰인 메시지까지 읽었는지 판단
 */
public record DirectMessageReadEvent(
    UUID conversationId,
    UUID readerId,
    Instant readAt
) {

  public static DirectMessageReadEvent of(UUID conversationId, UUID readerId, Instant readAt) {
    return new DirectMessageReadEvent(conversationId, readerId, readAt);
  }
}
