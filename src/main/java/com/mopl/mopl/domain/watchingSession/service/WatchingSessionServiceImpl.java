package com.mopl.mopl.domain.watchingSession.service;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionChange;
import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.entity.ChangeType;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import com.mopl.mopl.domain.watchingSession.exception.WatchingSessionAlreadyJoinedException;
import com.mopl.mopl.domain.watchingSession.mapper.WatchingSessionMapper;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepository;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    private final WatchingSessionMapper sessionMapper;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void join(UUID contentId, UUID userId) {

        // 이미 시청 중인 세션이 있는가?
        if (watchingSessionRepository.existsByContentIdAndUserId(contentId, userId)) {
            throw new WatchingSessionAlreadyJoinedException(contentId, userId);
        }

        // TODO: 유저 커스텀 예외 생성 후 추가
        User user = userRepository.findById(userId)
                .orElseThrow();
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        WatchingSession session = WatchingSession.builder()
                .content(content)
                .user(user)
                .build();
        watchingSessionRepository.save(session);

        long watcherCount = watchingSessionRepository.countByContentId(contentId);

        WatchingSessionDto sessionDto = sessionMapper.toDto(session.getId(), session.getCreatedAt(), content, user);

        WatchingSessionChange change = new WatchingSessionChange(
                ChangeType.JOIN,
                sessionDto,
                watcherCount
        );
        eventPublisher.publishEvent(new WatchingSessionEvent(change, contentId));
    }

    @Override
    @Transactional
    public void leave(UUID contentId, UUID userId) {

        // TODO: 유저 커스텀 예외 생성 후 추가
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException();
        }

        if (!contentRepository.existsById(contentId)) {
            throw new ContentNotFoundException(contentId);
        }

        watchingSessionRepository.deleteByContentIdAndUserId(contentId, userId);

        long watcherCount = watchingSessionRepository.countByContentId(contentId);
        WatchingSessionChange change = new WatchingSessionChange(
                ChangeType.LEAVE,
                null,
                watcherCount
        );
        eventPublisher.publishEvent(new WatchingSessionEvent(change, contentId));
    }
}
