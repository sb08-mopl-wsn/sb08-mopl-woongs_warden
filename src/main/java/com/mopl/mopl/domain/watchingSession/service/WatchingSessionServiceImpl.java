package com.mopl.mopl.domain.watchingSession.service;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionChange;
import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.entity.ChangeType;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import com.mopl.mopl.domain.watchingSession.exception.WatchingSessionAlreadyJoinedException;
import com.mopl.mopl.domain.watchingSession.exception.WatchingSessionNotFoundException;
import com.mopl.mopl.domain.watchingSession.mapper.WatchingSessionMapper;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepository;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class WatchingSessionServiceImpl implements WatchingSessionService {

    // Repository
    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    // Mapper
    private final WatchingSessionMapper sessionMapper;

    // Event
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void join(UUID contentId, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        WatchingSession session = WatchingSession.builder()
                .content(content)
                .user(user)
                .build();

        // JOIN 중복 방지가 원자적이지 않아 동시 요청에서 중복 세션 또는 500 에러가 발생할 수 있음.
        try {
            watchingSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException e) {
            throw new WatchingSessionAlreadyJoinedException(contentId, userId);
        }

        WatchingSessionDto sessionDto = sessionMapper.toDto(session.getId(), session.getCreatedAt(), content, user);
        publishSessionEvent(contentId, ChangeType.JOIN, sessionDto);
    }

    @Override
    @Transactional
    public void leave(UUID contentId, UUID userId) {

        WatchingSession session = watchingSessionRepository.findByContentIdAndUserId(contentId, userId)
                .orElseThrow(() -> new WatchingSessionNotFoundException("현재 시청 중인 세션이 없습니다."));

        validateUserAndContent(contentId, userId);

        // 퇴장 처리를 위해 이전 세션을 저장해놓음.
        WatchingSessionDto sessionDto = sessionMapper.toDto(
                session.getId(),
                session.getCreatedAt(),
                session.getContent(),
                session.getUser()
        );

        watchingSessionRepository.delete(session);
        // 쓰기 지연 방지 및 동기화
        watchingSessionRepository.flush();

        publishSessionEvent(contentId, ChangeType.LEAVE, sessionDto);
    }

    private void publishSessionEvent(UUID contentId, ChangeType type, WatchingSessionDto sessionDto) {
        long watcherCount = watchingSessionRepository.countByContentId(contentId);

        WatchingSessionChange change = new WatchingSessionChange(
                type,
                sessionDto,
                watcherCount
        );
        eventPublisher.publishEvent(new WatchingSessionEvent(change, contentId));
    }

    private void validateUserAndContent(UUID contentId, UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        if (!contentRepository.existsById(contentId)) {
            throw new ContentNotFoundException(contentId);
        }
    }
}