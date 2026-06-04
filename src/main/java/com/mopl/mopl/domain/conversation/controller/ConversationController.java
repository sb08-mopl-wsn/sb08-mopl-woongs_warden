package com.mopl.mopl.domain.conversation.controller;

import com.mopl.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.request.CursorConversationRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
import com.mopl.mopl.domain.conversation.service.ConversationService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController implements ConversationApi {

  private final ConversationService conversationService;

  // 대화방 생성 혹은 기존 대화방 조회
  @PostMapping
  public ResponseEntity<ConversationDto> createConversation(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @Valid @RequestBody ConversationCreateRequest request
  ) {
    UUID currentUserId = userDetails.getUserDto().id();
    ConversationDto response = conversationService.createConversation(currentUserId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 내 대화방 목록 조회
  @GetMapping
  public ResponseEntity<CursorResponseConversationDto> getMyConversations(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @Valid @ModelAttribute CursorConversationRequest request
  ) {
    UUID currentUserId = userDetails.getUserDto().id();
    CursorResponseConversationDto response = conversationService.getMyConversations(currentUserId, request);

    return ResponseEntity.ok(response);
  }

  // 특정 대화방 상세 조회
  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> getConversation(
    @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
    @PathVariable("conversationId") UUID conversationId
  ) {
    UUID currentUserId = userDetails.getUserDto().id();
    ConversationDto response = conversationService.getConversation(currentUserId, conversationId);

    return ResponseEntity.ok(response);
  }

  // 특정 상대와의 대화방 조회
  @GetMapping("/with")
  public ResponseEntity<ConversationDto> getConversationWith(
    @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
    @RequestParam("userId") UUID withUserId
  ) {
    UUID currentUserId = userDetails.getUserDto().id();
    ConversationDto response = conversationService.getConversationWith(currentUserId, withUserId);

    return ResponseEntity.ok(response);
  }

}
