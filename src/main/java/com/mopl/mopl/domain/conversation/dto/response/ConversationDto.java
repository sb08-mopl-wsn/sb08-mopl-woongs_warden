package com.mopl.mopl.domain.conversation.dto.response;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

/**
 * 대화방 DTO
 * @param id 대화방 id
 * @param with 나와 대화 중인 상대방의 정보
 * @param lastestMessage 가장 최근 메시지
 * @param hasUnread 안 읽은 메시지가 있는지 여부
 * @param opponentLastReadAt 상대방이 마지막으로 읽은 메시지 시간
 */
public record ConversationDto(
    UUID id,
    UserSummary with,
    DirectMessageDto lastestMessage,
    boolean hasUnread,
    Instant opponentLastReadAt
) {

}
