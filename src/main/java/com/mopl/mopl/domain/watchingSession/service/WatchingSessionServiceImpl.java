package com.mopl.mopl.domain.watchingSession.service;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.domain.watchingSession.dto.request.ContentChatSendRequest;
import com.mopl.mopl.domain.watchingSession.dto.request.WatchingSessionPageRequest;
import com.mopl.mopl.domain.watchingSession.dto.response.*;
import com.mopl.mopl.domain.watchingSession.entity.ChangeType;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import com.mopl.mopl.domain.watchingSession.exception.WatchingSessionNotFoundException;
import com.mopl.mopl.domain.watchingSession.mapper.WatchingSessionMapper;
import com.mopl.mopl.domain.watchingSession.repository.WatchingSessionRepository;
import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    /**
     * 콘텐츠 실시간 웹소켓 접속을 위한 로직
     *
     * @param contentId 콘텐츠 시청 세션 참여를 위한 콘텐츠 ID
     * @param userId    콘텐츠 시청 세션 참여를 위한 유저 ID
     */
    @Override
    @Transactional
    public void join(UUID contentId, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        WatchingSession session = watchingSessionRepository
                .findByContentIdAndUserId(contentId, userId)
                .orElseGet(() -> {
                    WatchingSession newSession = WatchingSession.builder()
                            .content(content)
                            .user(user)
                            .build();
                    return watchingSessionRepository.saveAndFlush(newSession);
                });

        WatchingSessionDto sessionDto = sessionMapper.toDto(session.getId(), session.getCreatedAt(), content, user);
        publishSessionEvent(contentId, ChangeType.JOIN, sessionDto);
    }

    /**
     * 콘텐츠 실시간 웹소켓 접속 해제를 위한 로직
     *
     * @param contentId 콘텐츠 시청 세션 퇴장을 위한 콘텐츠 ID
     * @param userId    콘텐츠 시청 세션 퇴장을 위한 유저 ID
     * @throws WatchingSessionNotFoundException 시청 세션이 존재하지 않을 경우 발생
     */
    @Override
    @Transactional
    public void leave(UUID contentId, UUID userId) {

        WatchingSession session = watchingSessionRepository.findByContentIdAndUserId(contentId, userId)
                .orElseThrow(() -> new WatchingSessionNotFoundException("현재 시청 중인 세션이 없습니다."));

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

    /**
     * 실시간 채팅 메시지를 수신하여 검증한 후, 채팅 이벤트를 발행한다.
     *
     * @param contentId 채팅이 발생한 콘텐츠 ID
     * @param senderId  메시지를 보낸 사용자의 ID
     * @param request   메시지 내용이 담긴 DTO
     * @throws UserNotFoundException    사용자가 존재하지 않을 경우 발생
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 경우 발생
     */
    @Override
    public void receiveMessage(UUID contentId, UUID senderId, ContentChatSendRequest request) {

        User user = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        if (!contentRepository.existsById(contentId)) {
            throw new ContentNotFoundException(contentId);
        }

        UserSummary sender = new UserSummary(
                user.getId(),
                user.getName(),
                user.getProfileImageKey()
        );

        ContentChatDto chatDto = sessionMapper.toChatDto(sender, request.content());

        eventPublisher.publishEvent(new LiveChatEvent(contentId, chatDto));
    }

    /**
     * 특정 콘텐츠의 시청 세션 목록을 커서 기반 페이징으로 조회한다.
     * 성능 최적화를 위해 No-Offset(Slice) 방식과 QueryDsl를 사용한다.
     *
     * @param contentId 시청 세션을 조회할 콘텐츠 ID
     * @param request   커서, 유저 이름, 페이지 크기, 정렬 기준, 정렬 방향이 담긴 DTO
     * @return 다음 커서 정보와 시청자 목록, 전체 카운트를 포함한 CursorResponseWatchingSessionDto
     */
    @Override
    public CursorResponseWatchingSessionDto findByContentInWatchingSession(UUID contentId, WatchingSessionPageRequest request) {

        int limit = request.limit();
        Pageable pageable = PageRequest.of(0, limit + 1);

        WatchingSessionSearchCondition condition = new WatchingSessionSearchCondition(
                contentId,
                request.cursor(),
                request.watcherNameLike(),
                request.idAfter(),
                request.sortDirection()
        );

        List<WatchingSession> watchingSessionList = watchingSessionRepository.findAllByCursor(
                condition,
                pageable
        );

        // Slice
        boolean hasNext = false;
        if (watchingSessionList.size() > limit) {
            hasNext = true;
            watchingSessionList.remove(watchingSessionList.size() - 1);
        }

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!watchingSessionList.isEmpty()) {
            WatchingSession lastSession = watchingSessionList.get(watchingSessionList.size() - 1);
            nextCursor = lastSession.getCreatedAt().toString();
            nextIdAfter = lastSession.getId();
        }

        List<WatchingSessionDto> data = watchingSessionList.stream()
                .map(sessionMapper::toDto)
                .toList();

        // TODO: 잦은 조회로 성능 문제 발생하므로 캐싱 처리하겠습니다.
        long totalCount = watchingSessionRepository.countByContentId(contentId);

        return new CursorResponseWatchingSessionDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                request.sortBy(),
                request.sortDirection()
        );
    }

    // 시청 세션 이벤트 발행
    private void publishSessionEvent(UUID contentId, ChangeType type, WatchingSessionDto sessionDto) {

        long watcherCount = watchingSessionRepository.countByContentId(contentId);

        WatchingSessionChange change = new WatchingSessionChange(
                type,
                sessionDto,
                watcherCount
        );
        eventPublisher.publishEvent(new WatchingSessionEvent(change, contentId));
    }
}