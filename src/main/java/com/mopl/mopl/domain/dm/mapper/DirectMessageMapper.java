package com.mopl.mopl.domain.dm.mapper;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.dm.entity.DirectMessage;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ImageUrlConverter.class)
public interface DirectMessageMapper {

  @Mapping(source = "conversation.id", target = "conversationId")
  @Mapping(source = "sender", target = "sender")
  @Mapping(expression = "java(extractReceiver(message))", target = "receiver")
  DirectMessageDto toDto(DirectMessage message);

  @Mapping(source = "id", target = "userId")
  @Mapping(source = "profileImageKey", target = "profileImageUrl", qualifiedByName = "toThumbnailUrl")
  UserSummary toUserSummary(User user);

  default UserSummary extractReceiver(DirectMessage message) {
    // 1. 깊은 Null 체크 (NPE 방지)
    if (message == null || message.getConversation() == null || message.getSender() == null
        || message.getConversation().getSender() == null
        || message.getConversation().getReceiver() == null) {
      return null;
    }

    User conversationSender = message.getConversation().getSender();
    User conversationReceiver = message.getConversation().getReceiver();
    User messageSender = message.getSender();

    // 2. 메시지 발신자가 방의 발신자와 같으면, 수신자는 방의 수신자
    if (messageSender.getId().equals(conversationSender.getId())) {
      return toUserSummary(conversationReceiver);
    }

    // 3. 메시지 발신자가 방의 수신자와 같으면, 수신자는 방의 발신자
    if (messageSender.getId().equals(conversationReceiver.getId())) {
      return toUserSummary(conversationSender);
    }

    // 4. 둘다 아니면 데이터 오염상태. 명시적 예외 발생
    throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "메시지 발신자가 해당 대화방의 참여자가 아닙니다. 데이터 무결성 오류.");
  }
}
