package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;

import java.util.UUID;

/**
 * DM 전송 완료 시 발생하는 이벤트 객체
 * @param conversationId 저장된 대화방의 ID
 * @param messageDto 전송된 메시지 DTO
 */
public record DirectMessageSentEvent(
    UUID conversationId,
    DirectMessageDto messageDto
) {

  // 팩토리 메서드
  public static DirectMessageSentEvent of(UUID conversationId, DirectMessageDto messageDto) {
    return new DirectMessageSentEvent(conversationId, messageDto);
  }
}
