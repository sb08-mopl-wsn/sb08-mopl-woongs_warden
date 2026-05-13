package com.mopl.mopl.domain.dm.controller;

import com.mopl.mopl.domain.dm.dto.DirectMessageSendRequest;
import com.mopl.mopl.domain.dm.service.DirectMessageService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DirectMessageStompController {

    private final DirectMessageService directMessageService;

    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void handleDirectMessage(@DestinationVariable UUID conversationId,
                                    @Valid @Payload DirectMessageSendRequest request,
                                    @AuthenticationPrincipal MoplUserDetails userDetails) {
        directMessageService.sendMessage(userDetails.getUserDto().id(), conversationId, request);
    }
}
