package com.mopl.mopl.domain.watchingSession.controller;

import com.mopl.mopl.domain.watchingSession.dto.request.ContentChatSendRequest;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    /**
     * 실시간 채팅 메시지를 수신하여 처리한다.
     *
     * @param contentId 실시간 채팅이 진행 중인 콘텐츠의 ID
     * @param request 메시지 내용을 담은 DTO
     * @param principal 인증된 현재 사용자 정보
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void handleLiveChat(@DestinationVariable UUID contentId,
                               @Payload ContentChatSendRequest request,
                               Principal principal) {
        MoplUserDetails userDetails = extractUserDetails(principal);
        watchingSessionService.receiveMessage(contentId, userDetails.getUserDto().id(), request);
    }

    // TODO: 예외처리 추후 수정
    private MoplUserDetails extractUserDetails(Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        return (MoplUserDetails) auth.getPrincipal();
    }
}
