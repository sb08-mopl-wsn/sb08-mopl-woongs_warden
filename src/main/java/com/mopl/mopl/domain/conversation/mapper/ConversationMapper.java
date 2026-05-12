package com.mopl.mopl.domain.conversation.mapper;

import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

  /**
   * Entity -> Dto 변환 메서드
   * @param conversation 대화방 엔티티
   * @param currentUserId 이 데이터를 조회하는 현재 로그인 유저 ID (상대방을 가려내기 위해)
   * @return 변환된 Dto 객체
   */
  default ConversationDto toDto(Conversation conversation, UUID currentUserId) {

    if (conversation == null || currentUserId == null) return null;

    // 내가 sender라면 상대방은 receiver, 내가 receiver라면 상대방은 sender
    User withUser = conversation.getSender().getId().equals(currentUserId)
        ? conversation.getReceiver()
        : conversation.getSender();

    UserSummary withUserSummary = toUserSummary(withUser);

    // TODO: lastestMessage 매핑은 Message 도메인 로직 추가 후 연동 예정
    return new ConversationDto(
        conversation.getId(),
        withUserSummary,
        null,
        conversation.isHasUnread()
    );
  }

  @Mapping(source = "id", target = "userId")
  @Mapping(source = "profileImageKey", target = "profileImageUrl")
  UserSummary toUserSummary(User user);

}
