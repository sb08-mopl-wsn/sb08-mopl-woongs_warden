package com.mopl.mopl.domain.dm.controller;

import com.mopl.mopl.domain.dm.dto.CursorResponseDirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageSendRequest;
import com.mopl.mopl.domain.dm.service.DirectMessageService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{conversationId}/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

  private final DirectMessageService messageService;

  // 과거 채팅 내역 스크롤(조회)
  @GetMapping
  public ResponseEntity<CursorResponseDirectMessageDto> getMessages(
      @AuthenticationPrincipal(errorOnInvalidType = true)MoplUserDetails userDetails,
      @PathVariable("conversationId")UUID conversationId,
      @Valid @ModelAttribute CursorPaginationRequest request
  ) {

    UUID currentUserId = userDetails.getUserDto().id();
    CursorResponseDirectMessageDto response = messageService.getMessages(currentUserId, conversationId, request);
    return ResponseEntity.ok(response);
  }

  // 일반 REST API로 채팅 전송 (테스트용 / STOMP 환경 지원 클라이언트용)
  @PostMapping
  public ResponseEntity<DirectMessageDto> sendMessage(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @PathVariable("conversationId") UUID conversationId,
      @Valid @RequestBody DirectMessageSendRequest request
  ) {
    UUID currentUserId = userDetails.getUserDto().id();
    DirectMessageDto response = messageService.sendMessage(currentUserId, conversationId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 메시지 읽음 처리
  @PostMapping("/{directMessageId}/read")
  public ResponseEntity<Void> readMessage(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @PathVariable("conversationId") UUID conversationId,
      @PathVariable("directMessageId") UUID messageId
  ) {

    UUID currentUserId = userDetails.getUserDto().id();
    messageService.readMessage(currentUserId, conversationId, messageId);

    return ResponseEntity.ok().build();
  }
}
