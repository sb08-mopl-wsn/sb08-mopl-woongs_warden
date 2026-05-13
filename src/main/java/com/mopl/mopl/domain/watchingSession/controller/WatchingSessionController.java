package com.mopl.mopl.domain.watchingSession.controller;

import com.mopl.mopl.domain.watchingSession.dto.request.ContentChatSendRequest;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    /**
     * 실시간 채팅 메시지를 수신하여 처리한다.
     *
     * @param contentId   실시간 채팅이 진행 중인 콘텐츠의 ID
     * @param request     메시지 내용을 담은 DTO
     * @param userDetails 인증된 현재 사용자 정보
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void handleLiveChat(@DestinationVariable UUID contentId,
                               @Valid @Payload ContentChatSendRequest request,
                               @AuthenticationPrincipal MoplUserDetails userDetails) {
        watchingSessionService.receiveMessage(contentId, userDetails.getUserDto().id(), request);
    }
}