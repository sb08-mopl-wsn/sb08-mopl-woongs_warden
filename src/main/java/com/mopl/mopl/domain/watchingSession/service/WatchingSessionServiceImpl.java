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
import com.mopl.mopl.global.component.BadWordFilter;
import com.mopl.mopl.global.event.BadWordDetectedEvent;
import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // filtering
    private final BadWordFilter badWordFilter;

    // S3
    private final S3ImageStorage s3ImageStorage;

    // redis
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "content:watcher:count:";

    /**
     * 콘텐츠 실시간 웹소켓 접속을 위한 로직
     *
     * @param contentId 콘텐츠 시청 세션 참여를 위한 콘텐츠 ID
     * @param userId    콘텐츠 시청 세션 참여를 위한 유저 ID
     */
    @CacheEvict(value = "contents", allEntries = true)
    @Override
    @Transactional
    public void join(UUID contentId, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        AtomicBoolean isNew = new AtomicBoolean(false);

        WatchingSession session = watchingSessionRepository
                .findByContentIdAndUserId(contentId, userId)
                .orElseGet(() -> {
                    isNew.set(true);
                    WatchingSession newSession = WatchingSession.builder()
                            .content(content)
                            .user(user)
                            .build();
                    return watchingSessionRepository.saveAndFlush(newSession);
                });

        if (isNew.get()) {
            content.updateWatcherCount(content.getWatcherCount() + 1);

            // Redis 분산 카운터 증가 (Atomic INCR)
            String redisKey = REDIS_KEY_PREFIX + contentId;
            redisTemplate.opsForValue().increment(redisKey);
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        }

        WatchingSessionDto sessionDto = sessionMapper.toDto(
                session.getId(),
                session.getCreatedAt(),
                content,
                user
        );
        publishSessionEvent(contentId, ChangeType.JOIN, sessionDto, content.getWatcherCount());
    }

    /**
     * 콘텐츠 실시간 웹소켓 접속 해제를 위한 로직
     * 시청 세션이 존재하지 않을 경우 예외를 발생시키는 대신 무시하도록 변경하여, 중복 호출에도 안전하게 동작하도록 처리
     *
     * @param contentId 콘텐츠 시청 세션 퇴장을 위한 콘텐츠 ID
     * @param userId    콘텐츠 시청 세션 퇴장을 위한 유저 ID
     */
    @CacheEvict(value = "contents", allEntries = true)
    @Override
    @Transactional
    public void leave(UUID contentId, UUID userId) {

        watchingSessionRepository.findByContentIdAndUserId(contentId, userId)
                .ifPresent(session -> {

                    WatchingSessionDto sessionDto = sessionMapper.toDto(
                            session.getId(),
                            session.getCreatedAt(),
                            session.getContent(),
                            session.getUser()
                    );

                    watchingSessionRepository.delete(session);
                    watchingSessionRepository.flush();

                    Content content = session.getContent();
                    // 음수 방지
                    content.updateWatcherCount(Math.max(0, content.getWatcherCount() - 1));

                    // Redis 분산 카운터 감소 (Atomic DECR)
                    String redisKey = REDIS_KEY_PREFIX + contentId;
                    Long currentCount = redisTemplate.opsForValue().decrement(redisKey);
                    // 음수 방지
                    if (currentCount != null && currentCount < 0) {
                        redisTemplate.opsForValue().set(redisKey, "0");
                    }

                    publishSessionEvent(contentId, ChangeType.LEAVE, sessionDto, content.getWatcherCount());
                });
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

        if (user.isBanned()) {
            log.info("정지 당한 유저는 실시간 채팅에 참여할 수 없습니다. senderId: {}", senderId);
            return;
        }

        // 채팅 송신자에 대한 시청 세션 참여 검증
        // 해당 콘텐츠를 시청 중이지 않은 사용자도 임의의 콘텐츠 채팅에 메시지를 보낼 수 있어 권한 경계가 무너질 수 있음.
        if (watchingSessionRepository.findByContentIdAndUserId(contentId, senderId).isEmpty()) {
            throw new WatchingSessionNotFoundException(contentId, senderId);
        }

        String imageUrl = null;
        if (user.getProfileImageKey() != null && !user.getProfileImageKey().isBlank()) {
            imageUrl = s3ImageStorage.getPublicUrl(user.getProfileImageKey());
        }

        UserSummary sender = new UserSummary(
                user.getId(),
                user.getName(),
                imageUrl
        );

        String originalMessage = request.content();

        String filteredMessage = badWordFilter.maskBadWord(originalMessage);

        if (!filteredMessage.equals(request.content())) {
            eventPublisher.publishEvent(new BadWordDetectedEvent(senderId, originalMessage));
        }

        ContentChatDto chatDto = sessionMapper.toChatDto(sender, filteredMessage);

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

        String redisKey = REDIS_KEY_PREFIX + contentId;
        String cachedCount = redisTemplate.opsForValue().get(redisKey);

        long totalCount;
        if (cachedCount != null) {
            totalCount = Long.parseLong(cachedCount);
        } else {
            log.debug("캐시가 만료되어 RDB fallback이 진행됩니다.");
            // 서버 분산 환경이나 Redis 재시작 등으로 캐시가 만료되었을 때만 RDB fallback 수행
            totalCount = watchingSessionRepository.countByContentId(contentId);
            redisTemplate.opsForValue().set(redisKey, String.valueOf(totalCount), 1, TimeUnit.DAYS);
        }

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

    /**
     * 특정 유저가 현재 시청 중인 세션을 조회합니다.
     *
     * @param userId 조회할 유저 ID
     * @return 시청 세션 정보 (시청 중이 아니면 Empty)
     */
    @Override
    public Optional<WatchingSessionDto> findCurrentWatchingSessionByUserId(UUID userId, UUID currentUserId) {

        String userSessionKey = String.format("ws:user:%s:sessions", userId);
        Set<String> userSessions = redisTemplate.opsForSet().members(userSessionKey);

        // 유저의 활성화된 웹소켓 세션이 아예 없다면 RDB에 조회없이 시청 중이 아님을 즉시 반환
        if (userSessions == null || userSessions.isEmpty()) {
            return Optional.empty();
        }

        UUID activeContentId = null;

        // 유저 세션들을 순회하며 현재 시청 중인 콘텐츠 ID를 역추적한다.
        for (String sessionId : userSessions) {
            String sessionContentKey = String.format("ws:session:%s:contents", sessionId);
            Set<String> contentIds = redisTemplate.opsForSet().members(sessionContentKey);

            if (contentIds != null && !contentIds.isEmpty()) {
                String targetContentIdStr = contentIds.iterator().next();
                activeContentId = UUID.fromString(targetContentIdStr);
                break;
            }
        }

        // Redis 상에서 시청중인 룸 정보가 없다면 퇴장한 것을 판단한다.
        if (activeContentId == null) {
            return Optional.empty();
        }

        // RDB Fullback 1회 조회
        // redis에서 activeContentId를 확실하게 캐치하므로
        // 매핑에 필요한 순수 도메인 DTO만 데이터베이스에서 정확하게 1번 가져온다.
        return watchingSessionRepository.findByContentIdAndUserId(activeContentId, userId)
                .map(sessionMapper::toDto);
    }

    // 시청 세션 이벤트 발행
    private void publishSessionEvent(UUID contentId, ChangeType type, WatchingSessionDto sessionDto, long watcherCount) {

        WatchingSessionChange change = new WatchingSessionChange(
                type,
                sessionDto,
                watcherCount
        );
        eventPublisher.publishEvent(new WatchingSessionEvent(change, contentId));
    }
}