package com.mopl.mopl.global.event;

import java.util.UUID;

/**
 * DM 전송 완료 시 발생하는 이벤트 객체
 * @param messageId 저장된 메시지의 ID
 * @param receiverId 알림을 받을 상대방의 Id
 * @param content 알림을 띄워줄 메시지 내용 미리보기
 */
public record DirectMessageCreatedEvent(
    UUID messageId,
    UUID receiverId,
    String content
) {

}
