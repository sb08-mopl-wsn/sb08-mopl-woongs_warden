package com.mopl.mopl.domain.conversation.service;

import com.mopl.mopl.domain.conversation.dto.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import java.util.UUID;

public interface ConversationService {

  // 대화방 생성 (이미 존재하면 기존 방 반환)
  ConversationDto createConversation(UUID currentUserId, ConversationCreateRequest request);

  // 내 대화방 목록 조회 (커서 페이징)
  CursorResponseConversationDto getMyConversations(UUID currentUserId, CursorPaginationRequest request);

  // 특정 대화방 상세 조회
  ConversationDto getConversation(UUID currentUserId, UUID conversationId);

  // 특정 상대와의 대화방 조회
  ConversationDto getConversationWith(UUID currentUserId, UUID withUserId);
}
