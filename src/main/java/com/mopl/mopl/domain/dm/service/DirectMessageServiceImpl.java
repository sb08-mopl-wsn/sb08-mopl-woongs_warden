package com.mopl.mopl.domain.dm.service;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.conversation.exception.ConversationAccessDeniedException;
import com.mopl.mopl.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.mopl.domain.dm.dto.CursorResponseDirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageSendRequest;
import com.mopl.mopl.domain.dm.mapper.DirectMessageMapper;
import com.mopl.mopl.domain.dm.repository.DirectMessageRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.domain.dm.entity.DirectMessage;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.DirectMessageSentEvent;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageServiceImpl implements DirectMessageService{

  private final DirectMessageRepository messageRepository;
  private final ConversationRepository conversationRepository;
  private final UserRepository userRepository;
  private final DirectMessageMapper messageMapper;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public DirectMessageDto sendMessage(UUID currentUserId, UUID conversationId, DirectMessageSendRequest request) {

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationNotFoundException(conversationId));

    validateConversationAccess(conversation, currentUserId);

    User sender = userRepository.findById(currentUserId)
        .orElseThrow(() -> new UserNotFoundException(currentUserId));

    // 메시지 저장
    DirectMessage message = DirectMessage.builder()
        .conversation(conversation)
        .sender(sender)
        .content(request.content())
        .build();
    DirectMessage savedMessage = messageRepository.save(message);

    // 대화방 읽지 않음 상태(hasUnread), 업데이트 시간(Dirty Checking) 갱신
    conversation.updateUnreadStatus(true);

    // 상대방 찾아서 SSE 알림 푸시
    UUID receiverId = conversation.getSender().getId().equals(currentUserId)
        ? conversation.getReceiver().getId()
        : conversation.getSender().getId();

    // TODO: EventPublisher 형식으로 디커플링 가능
    eventPublisher.publishEvent(new DirectMessageCreatedEvent(
        savedMessage.getId(),
        receiverId,
        request.content()
    ));

    eventPublisher.publishEvent(new DirectMessageSentEvent(
            conversationId,
            messageMapper.toDto(savedMessage)
    ));

    return messageMapper.toDto(savedMessage);
  }

  @Override
  public CursorResponseDirectMessageDto getMessages(UUID currentUserId, UUID conversationId, CursorPaginationRequest request) {

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationNotFoundException(conversationId));

    validateConversationAccess(conversation, currentUserId);

    if (request.limit() == null || request.limit() <= 0) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "limit은 1 이상의 값이어야 합니다.");
    }

    boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
    boolean hasIdAfter = request.idAfter() != null;
    if (hasCursor != hasIdAfter) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "cursor와 idAfter는 항상 함께 전달되어야 합니다.");
    }

    // 정렬 파라미터 검증
    if (!"createdAt".equals(request.sortBy())) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "정렬 기준(sortBy)은 'createdAt'만 지원합니다.");
    }
    if (!"ASCENDING".equalsIgnoreCase(request.sortDirection()) && !"DESCENDING".equalsIgnoreCase(request.sortDirection())) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "정렬 방향은 'ASCENDING' 또는 'DESCENDING'만 지원합니다.");
    }

    // 커서 파싱
    Instant cursorTime = null;
    if (request.cursor() != null && !request.cursor().isBlank()) {
      try {
        cursorTime = Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 형식의 커서 데이터입니다.");
      }
    }

    // DB 조회
    List<DirectMessage> messages = messageRepository.findMessagesByCursor(
        conversationId, cursorTime, request.idAfter(), request.limit(), request.sortDirection());

    // 다음 페이지 존재 여부, 잘라내기
    boolean hasNext = messages.size() > request.limit();
    if (hasNext) {
      messages.remove(request.limit().intValue());
    }

    // 커서 정보 갱신
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (!messages.isEmpty()) {
      DirectMessage lastMessage = messages.get(messages.size() -1);
      nextCursor = lastMessage.getCreatedAt().toString();
      nextIdAfter = lastMessage.getId();
    }

    // 총 개수
    long totalCount = -1L;
    if (request.cursor() == null || request.cursor().isBlank()) {
      totalCount = messageRepository.countByConversationId(conversationId);
    }

    // DTO 변환
    List<DirectMessageDto> data = messages.stream()
        .map(messageMapper::toDto)
        .toList();

    return new CursorResponseDirectMessageDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, request.sortBy(),
        request.sortDirection()
    );
  }

  private void validateConversationAccess(Conversation conv, UUID userId) {
    if (!conv.getSender().getId().equals(userId) && !conv.getReceiver().getId().equals(userId)) {
      throw new ConversationAccessDeniedException();
    }
  }
}
