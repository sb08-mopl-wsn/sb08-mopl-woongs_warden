package com.mopl.mopl.domain.dm.service;

import com.mopl.mopl.domain.dm.dto.CursorResponseDirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageSendRequest;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import java.util.UUID;

public interface DirectMessageService {

  // 메시지 1개 발송
  DirectMessageDto sendMessage(UUID currentUserId, UUID conversationId, DirectMessageSendRequest request);

  // 이전 대화 내역 불러오기
  CursorResponseDirectMessageDto getMessages(UUID currentUserId, UUID conversationId, CursorPaginationRequest request);

  // 대화방의 읽음 상태 업데이트
  void readMessage(UUID currentUserId, UUID conversationId, UUID messageId);
}
