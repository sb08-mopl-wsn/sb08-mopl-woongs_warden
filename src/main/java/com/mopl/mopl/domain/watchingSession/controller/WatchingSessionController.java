package com.mopl.mopl.domain.watchingSession.controller;

import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionChange;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    @MessageMapping("/contents/{contentId}/watch/join")
    public void handleJoin(@DestinationVariable UUID contentId,
                               @AuthenticationPrincipal MoplUserDetails userDetails) {

        UUID userId = userDetails.getUserDto().id();
        watchingSessionService.join(contentId, userId);
    }

    @MessageMapping("/contents/{contentId}/watch/leave")
    public void handleLeave(@DestinationVariable UUID contentId,
                            @AuthenticationPrincipal MoplUserDetails userDetails) {

        UUID userId = userDetails.getUserDto().id();
        watchingSessionService.leave(contentId, userId);
    }
}
