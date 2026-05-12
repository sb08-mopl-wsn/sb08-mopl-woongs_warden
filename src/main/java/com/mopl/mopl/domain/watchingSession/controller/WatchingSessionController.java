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
     * @param request   메시지 내용을 담은 DTO
     * @param principal 인증된 현재 사용자 정보
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void handleLiveChat(@DestinationVariable UUID contentId,
                               @Payload ContentChatSendRequest request,
                               Principal principal) {
        MoplUserDetails userDetails = extractUserDetails(principal);
        watchingSessionService.receiveMessage(contentId, userDetails.getUserDto().id(), request);
    }

    // TODO: 추후 예외 처리 구현할 예정
    private MoplUserDetails extractUserDetails(Principal principal) {
        // 1. Principal 존재 여부 확인
        if (principal == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // 2. 인증 객체 타입 확인 (패턴 매칭 사용)
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)) {
            throw new IllegalStateException("지원하지 않는 인증 타입입니다.");
        }

        // 3. UserDetails 타입 확인
        if (!(auth.getPrincipal() instanceof MoplUserDetails userDetails)) {
            throw new IllegalStateException("유효하지 않은 사용자 정보입니다.");
        }

        return userDetails;
    }
}