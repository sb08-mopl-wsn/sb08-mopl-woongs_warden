package com.mopl.mopl.domain.conversation.dto.response;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    UserSummary with,                 // 나와 대화 중인 상대방의 정보
    DirectMessageDto lastestMessage,  // 가장 최근 메시지
    boolean hasUnread                 // 안 읽은 메시지가 있는지 여부
) {

}
