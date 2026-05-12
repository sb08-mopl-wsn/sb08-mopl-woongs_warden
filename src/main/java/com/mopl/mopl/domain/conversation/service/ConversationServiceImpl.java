package com.mopl.mopl.domain.conversation.service;

import com.mopl.mopl.domain.conversation.dto.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.conversation.exception.ConversationAccessDeniedException;
import com.mopl.mopl.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.mopl.domain.conversation.mapper.ConversationMapper;
import com.mopl.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationServiceImpl implements ConversationService{

  private final ConversationRepository conversationRepository;
  private final UserRepository userRepository;
  private final ConversationMapper conversationMapper;

  @Override
  @Transactional
  public ConversationDto createConversation(UUID currentUserId, ConversationCreateRequest request) {

    if (currentUserId.equals(request.withUserId())) {
      throw new ConversationAccessDeniedException("자기 자신과는 대화방을 만들 수 없습니다.");
    }

    User sender = userRepository.findById(currentUserId)
        .orElseThrow(() -> new UserNotFoundException(currentUserId));
    User receiver = userRepository.findById(request.withUserId())
        .orElseThrow(() -> new UserNotFoundException(request.withUserId()));

    // 이미 둘 사이에 대화방이 있는지 확인
    Optional<Conversation> existingConv = conversationRepository.findConversationBetweenUsers(currentUserId, request.withUserId());

    if (existingConv.isPresent()) {
      return conversationMapper.toDto(existingConv.get(), currentUserId); // 기존 방 리턴
    }

    // 없으면 새로 생성
    Conversation newConv = Conversation.builder()
        .sender(sender)
        .receiver(receiver)
        .build();

    Conversation savedConv = conversationRepository.save(newConv);
    return conversationMapper.toDto(savedConv, currentUserId);
  }

  @Override
  public CursorResponseConversationDto getMyConversations(UUID currentUserId, CursorPaginationRequest request) {

    // 정렬 파라미터 검증
    if (!"updatedAt".equals(request.sortBy())) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "정렬 기준(sortBy)은 'updatedAt'만 지원합니다.");
    }
    if (!"ASCENDING".equalsIgnoreCase(request.sortDirection()) && !"DESCENDING".equalsIgnoreCase(request.sortDirection())) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "정렬 방향(sortDirection)은 'ASCENDING' 또는 'DESCENDING'만 지원합니다.");
    }

    // 커서 시간 파싱
    Instant cursorTime = null;
    if (request.cursor() != null && !request.cursor().isBlank()) {
      try {
        cursorTime = Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "잘못된 형식의 커서 데이터입니다.");
      }
    }

    // DB 조회
    PageRequest pageRequest = PageRequest.of(0, request.limit() + 1);
    List<Conversation> conversations;

    if ("ASCENDING".equalsIgnoreCase(request.sortDirection())) {
      conversations = conversationRepository.findMyConversationsByCursorAsc(currentUserId, cursorTime, request.idAfter(), pageRequest);
    } else {
      conversations = conversationRepository.findMyConversationsByCursorDesc(currentUserId, cursorTime, request.idAfter(), pageRequest);
    }

    // 다음 페이지 존재 확인
    boolean hasNext = conversations.size() > request.limit();
    if (hasNext) {
      conversations.remove(request.limit().intValue());
    }

    // 다음 페이지 조회 커서 정보
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (!conversations.isEmpty()) {
      Conversation lastConv = conversations.get(conversations.size() -1);
      nextCursor = lastConv.getUpdatedAt().toString();
      nextIdAfter = lastConv.getId();
    }

    // 사용자 총 대화방 개수
    long totalCount = -1L;
    if (request.cursor() == null || request.cursor().isBlank()) {
      totalCount = conversationRepository.countMyConversations(currentUserId);
    }

    // DTO 변환
    List<ConversationDto> data = conversations.stream()
        .map(conv -> conversationMapper.toDto(conv, currentUserId))
        .toList();

    return new CursorResponseConversationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, request.sortBy(), request.sortDirection()
    );
  }

  @Override
  public ConversationDto getConversation(UUID currentUserId, UUID conversationId) {
    Conversation conv = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ConversationNotFoundException(conversationId));

    validateConversationAccess(conv, currentUserId);

    return conversationMapper.toDto(conv, currentUserId);
  }

  @Override
  public ConversationDto getConversationWith(UUID currentUserId, UUID withUserId) {

    Conversation conv = conversationRepository.findConversationBetweenUsers(currentUserId, withUserId)
        .orElseThrow(() -> new ConversationNotFoundException(withUserId)); // 상대방과의 방이 없을 때

    return conversationMapper.toDto(conv, currentUserId);
  }

  private void validateConversationAccess(Conversation conv, UUID userId) {
    if (!conv.getSender().getId().equals(userId) && !conv.getReceiver().getId().equals(userId)) {
      throw new ConversationAccessDeniedException();
    }
  }
}
