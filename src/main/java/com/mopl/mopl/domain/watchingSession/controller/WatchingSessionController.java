package com.mopl.mopl.domain.watchingSession.controller;

import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    // TODO: AuthenticationPrincipal이 아직 없기 때문에 테스트를 위해 주석이 없는 아래 코드를 사용하였습니다.
//    @SubscribeMapping("/contents/{contentId}/watch/join")
//    public void handleJoin(@DestinationVariable UUID contentId,
//                           @AuthenticationPrincipal MoplUserDetails userDetails) {
//
//        UUID userId = userDetails.getUserDto().id();
//        watchingSessionService.join(contentId, userId);
//    }
//
//    @MessageMapping("/contents/{contentId}/watch/leave")
//    public void handleLeave(@DestinationVariable UUID contentId,
//                            @AuthenticationPrincipal MoplUserDetails userDetails) {
//        UUID userId = userDetails.getUserDto().id();
//        watchingSessionService.leave(contentId, userId);
//    }

    @SubscribeMapping("/contents/{contentId}/watch")
    public void handleSubscribe(@DestinationVariable UUID contentId,
                                @Header("userId") UUID userId) {
        watchingSessionService.join(contentId, userId);
    }

    @MessageMapping("/contents/{contentId}/watch/leave")
    public void handleLeave(@DestinationVariable UUID contentId,
                            @Header("userId") UUID userId) {
        watchingSessionService.leave(contentId, userId);
    }
}
