package com.mopl.mopl.domain.watchingSession.service;

import com.mopl.mopl.domain.watchingSession.dto.request.ContentChatSendRequest;
import com.mopl.mopl.domain.watchingSession.dto.request.WatchingSessionPageRequest;
import com.mopl.mopl.domain.watchingSession.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;

import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionService {
    void join(UUID contentId, UUID userId);

    void leave(UUID contentId, UUID userId);

    void receiveMessage(UUID contentId, UUID senderId, ContentChatSendRequest request);

    CursorResponseWatchingSessionDto findByContentInWatchingSession(UUID contentId, WatchingSessionPageRequest request);

    WatchingSessionDto findCurrentWatchingSessionByUserId(UUID userId);
}