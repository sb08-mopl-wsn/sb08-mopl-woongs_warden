package com.mopl.mopl.domain.dm.mapper;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.dm.entity.DirectMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

  @Mapping(source = "conversation.id", target = "conversationId")
  @Mapping(source = "sender", target = "sender")
  @Mapping(expression = "java(extractReceiver(message))", target = "receiver")
  DirectMessageDto toDto(DirectMessage message);

  @Mapping(source = "id", target = "userId")
  @Mapping(source = "profileImageKey", target = "profileImageUrl")
  UserSummary toUserSummary(User user);

  default UserSummary extractReceiver(DirectMessage message) {
    if (message == null || message.getConversation() == null || message.getSender() == null) {
      return null;
    }

    // 내가 보낸 사람이라면 방의 수신자가 받는 사람, 아니면 내가 받는 사람
    User receiverUser = message.getConversation().getSender().getId().equals(message.getSender().getId())
        ? message.getConversation().getReceiver()
        : message.getConversation().getSender();

    return toUserSummary(receiverUser);
  }
}
