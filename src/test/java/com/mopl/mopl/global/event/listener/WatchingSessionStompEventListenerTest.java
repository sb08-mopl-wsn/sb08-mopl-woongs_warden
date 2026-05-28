package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.component.WebSocketSessionRegistry;
import com.mopl.mopl.global.event.UserLogoutEvent;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionStompEventListener 테스트")
public class WatchingSessionStompEventListenerTest {

    @InjectMocks
    private WatchingSessionStompEventListener stompEventListener;

    @Mock
    private WatchingSessionService sessionService;
    @Mock
    private WebSocketSessionRegistry registry;
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private UUID contentId;
    private UUID userId;
    private String sessionId;
    private String subscriptionId;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sessionId = "mock-session-id-1234";
        subscriptionId = "sub-id-1234";

        UserDto userDto = new UserDto(
                userId,
                Instant.now(),
                "test@test.com",
                "테스트 유저",
                "profile.jpg",
                Role.USER,
                false,
                false
        );
        MoplUserDetails userDetails = new MoplUserDetails(userDto, null, null);
        authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                null
        );

        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private Message<byte[]> createStompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(authenticationToken);

        accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, sessionId);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UserLogoutEvent mockLogoutEvent() {
        UserLogoutEvent event = mock(UserLogoutEvent.class);
        when(event.userId()).thenReturn(userId);
        return event;
    }

    @Nested
    @DisplayName("handleConnect()")
    class HandleConnect {

        @Test
        @DisplayName("인증된 사용자가 연결되면 Redis에 유저-세션 매핑이 등록된다.")
        void authenticatedUser_registersSessionUserMapping() {
            // given
            Message<byte[]> message = createStompMessage(StompCommand.CONNECT, null);

            // when
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            // then
            assertDoesNotThrow(() -> stompEventListener.handleConnect(event));

            verify(setOperations).add(String.format("ws:user:%s:sessions", userId), sessionId);
            verify(redisTemplate).expire(eq(String.format("ws:user:%s:sessions", userId)), any(Duration.class));
        }

        @Test
        @DisplayName("인증 정보가 없는 연결은 내부 예외를 삼키고 무시한다.")
        void unauthenticatedUser_ignored() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId(sessionId);
            accessor.setUser(null);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            // when
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            // then
            assertDoesNotThrow(() -> stompEventListener.handleConnect(event));
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("handleSubscribe()")
    class HandleSubscribe {

        @Test
        @DisplayName("구독 경로가 null이면 무시하고 서비스의 join()을 호출하지 않는다.")
        void nullDestination_doesNothing() {
            // given
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, null);
            SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

            // when
            stompEventListener.handleSubscribe(event);

            // then
            verifyNoInteractions(sessionService);
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("구독 경로가 맞지 않으면 무시하고 서비스의 join()을 호출하지 않는다.")
        void invalidDestination_doesNothing() {
            // given
            String invalidDestination = "/sub/contents/" + contentId + "/chat";
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, invalidDestination);
            SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

            // when
            stompEventListener.handleSubscribe(event);

            // then
            verifyNoInteractions(sessionService);
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("경로의 contentId가 UUID 형식이 아니면 BusinessException을 잡고 종료한다.")
        void invalidUUID_logsWarning() {
            // given
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/not-a-uuid/watch");

            // when
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            // then
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("분산 락 획득에 실패하면 중복 join 처리를 막기 위해 early return 한다.")
        void lockFailed_returnsEarly() {
            // when
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            verify(setOperations, never()).add(anyString(), anyString());
        }

        @Test
        @DisplayName("처음 시청하는 콘텐츠면 join()을 호출하고 락을 삭제한다.")
        void firstTimeWatching_callsJoinAndReleasesLock() {
            // when
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
            when(setOperations.members(anyString())).thenReturn(null); // isUserWatching = false

            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            // then
            verify(sessionService).join(contentId, userId);
            verify(redisTemplate).delete(String.format("lock:watch:%s:%s", userId, contentId)); // lock 삭제 검증
        }

        @Test
        @DisplayName("다른 세션에서 이미 시청 중이면 join()을 호출하지 않는다.")
        void alreadyWatching_doesNotCallJoin() {
            // when
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

            when(setOperations.members(anyString())).thenReturn(Set.of("other-session-id"));
            when(setOperations.isMember(anyString(), eq(contentId.toString()))).thenReturn(true);

            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            // then
            verify(sessionService, never()).join(any(), any());
            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("비즈니스 로직 예외 발생 시 예외를 잡고 락을 해제한다.")
        void joinThrowsBusinessException_catchesAndReleasesLock() {
            // when
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
            doThrow(new BusinessException(GlobalErrorCode.INVALID_INPUT)).when(sessionService).join(contentId, userId);

            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            // then
            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("알 수 없는 예외 발생 시 catch 블록에서 처리하고 락을 해제한다.")
        void joinThrowsException_catchesAndReleasesLock() {
            // when
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
            doThrow(new RuntimeException()).when(sessionService).join(contentId, userId);

            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, message));

            // then
            verify(redisTemplate).delete(anyString());
        }
    }

    @Nested
    @DisplayName("handleUnSubscribe()")
    class HandleUnSubscribe {

        @Test
        @DisplayName("Redis에 구독 기록이 없으면 조기 종료한다.")
        void noSubscriptionRecord_returnsEarly() {
            // when
            when(valueOperations.get(anyString())).thenReturn(null);

            Message<byte[]> message = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, message));

            // then
            verify(setOperations, never()).remove(anyString(), anyString());
        }

        @Test
        @DisplayName("해당 세션에 시청 중인 룸이 0개가 되면 Redis Key를 삭제하고 DB leave()를 호출한다.")
        void roomCountZero_deletesKeyAndCallsLeave() {
            // when
            when(valueOperations.get(anyString())).thenReturn(contentId.toString());
            when(setOperations.size(anyString())).thenReturn(0L);
            when(setOperations.members(anyString())).thenReturn(null); //

            Message<byte[]> message = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, message));

            // then
            verify(redisTemplate).delete(String.format("ws:session:%s:contents", sessionId));
            verify(sessionService).leave(contentId, userId);
        }

        @Test
        @DisplayName("해당 세션에 다른 룸 시청이 남아있으면 SessionContents Key를 삭제하지 않는다.")
        void roomCountGreaterThanZero_doesNotDeleteKey() {
            // when
            when(valueOperations.get(anyString())).thenReturn(contentId.toString());
            when(setOperations.size(anyString())).thenReturn(1L);

            Message<byte[]> message = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, message));

            // then
            verify(redisTemplate, never()).delete(String.format("ws:session:%s:contents", sessionId));
        }

        @Test
        @DisplayName("구독 취소 시 leave() 도중 예외가 발생해도 로직을 중단하지 않는다.")
        void leaveThrowsExceptions_areCaught() {
            // when
            when(valueOperations.get(anyString())).thenReturn(contentId.toString());
            when(setOperations.size(anyString())).thenReturn(0L);

            // then
            doThrow(new RuntimeException()).when(sessionService).leave(contentId, userId);
            Message<byte[]> message1 = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            assertDoesNotThrow(() -> stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, message1)));

            doThrow(new BusinessException(GlobalErrorCode.INVALID_INPUT)).when(sessionService).leave(contentId, userId);
            Message<byte[]> message2 = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            assertDoesNotThrow(() -> stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, message2)));
        }
    }

    @Nested
    @DisplayName("handleLeave()")
    class HandleLeave {

        @Test
        @DisplayName("웹소켓 단절 시 보유한 SubId들과 세션 기록을 지우고, 최종적으로 leave()를 호출한다.")
        void fullDisconnect_cleansUpAndCallsLeave() {
            // when
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId)))
                    .thenReturn(Set.of(contentId.toString()));
            when(setOperations.members(String.format("ws:session:%s:subs", sessionId)))
                    .thenReturn(Set.of("sub-1", "sub-2"));

            when(setOperations.size(anyString())).thenReturn(0L);

            Message<byte[]> message = createStompMessage(StompCommand.DISCONNECT, null);
            stompEventListener.handleLeave(new SessionDisconnectEvent(this, message, sessionId, null));

            // then
            verify(redisTemplate).delete(String.format("ws:sub:%s:sub-1", sessionId));
            verify(redisTemplate).delete(String.format("ws:sub:%s:sub-2", sessionId));
            verify(redisTemplate).delete(String.format("ws:user:%s:sessions", userId));
            verify(sessionService).leave(contentId, userId);
        }

        @Test
        @DisplayName("유저 ID 추출 실패 시 조기 종료한다.")
        void authNull_returnsEarly() {
            // when
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId))).thenReturn(Set.of());

            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            accessor.setSessionId(sessionId); // No user attached
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            stompEventListener.handleLeave(new SessionDisconnectEvent(this, message, sessionId, null));

            // then
            verify(setOperations, never()).remove(anyString(), anyString());
        }

        @Test
        @DisplayName("유저의 다른 세션이 남아있다면 User Session Key를 삭제하지 않는다.")
        void remainSessionCountGreaterThanZero_doesNotDeleteUserKey() {
            // when
            when(setOperations.members(anyString())).thenReturn(Set.of());
            when(setOperations.size(anyString())).thenReturn(1L);

            Message<byte[]> message = createStompMessage(StompCommand.DISCONNECT, null);
            stompEventListener.handleLeave(new SessionDisconnectEvent(this, message, sessionId, null));

            // then
            verify(redisTemplate, never()).delete(String.format("ws:user:%s:sessions", userId));
        }

        @Test
        @DisplayName("leave 호출 중 DB 에러가 발생해도 다른 방 처리를 계속 진행한다.")
        void exceptionInLeave_catchesAndContinues() {
            // given
            UUID contentId2 = UUID.randomUUID();

            // when
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId)))
                    .thenReturn(Set.of(contentId.toString(), contentId2.toString()));
            when(setOperations.size(anyString())).thenReturn(0L);

            doThrow(new RuntimeException("DB 에러")).when(sessionService).leave(contentId, userId);

            Message<byte[]> message = createStompMessage(StompCommand.DISCONNECT, null);
            stompEventListener.handleLeave(new SessionDisconnectEvent(this, message, sessionId, null));

            // then
            verify(sessionService).leave(contentId, userId);
            verify(sessionService).leave(contentId2, userId);
        }

        @Test
        @DisplayName("전체 프로세스 중 예기치 않은 에러 발생 시 catch 블록에서 로깅한다.")
        void outerException_isCaught() {
            // when
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId)))
                    .thenThrow(new RuntimeException("Redis Error"));

            Message<byte[]> message = createStompMessage(StompCommand.DISCONNECT, null);

            // then
            assertDoesNotThrow(() -> stompEventListener.handleLeave(new SessionDisconnectEvent(this, message, sessionId, null)));
        }
    }

    @Nested
    @DisplayName("handleUserLogout()")
    class HandleUserLogout {

        @Test
        @DisplayName("세션이 없으면 아무 동작도 하지 않는다.")
        void noSessions_skipsLogic() {
            // when
            when(setOperations.members(String.format("ws:user:%s:sessions", userId))).thenReturn(null);

            UserLogoutEvent event = new UserLogoutEvent(userId);
            stompEventListener.handleUserLogout(event);

            // then
            verify(redisTemplate, never()).delete(String.format("ws:session:%s:contents", sessionId));
            verifyNoInteractions(sessionService);
        }

        @Test
        @DisplayName("로그아웃 시 Redis Key 일괄 정리 및 Websocket 강제 종료, DB leave()를 수행한다.")
        void logout_cleansUpAndCloses() throws Exception {
            // when
            when(setOperations.members(String.format("ws:user:%s:sessions", userId))).thenReturn(Set.of(sessionId));
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId))).thenReturn(Set.of(contentId.toString()));
            when(setOperations.members(String.format("ws:session:%s:subs", sessionId))).thenReturn(Set.of("sub-1"));

            WebSocketSession mockWsSession = mock(WebSocketSession.class);
            when(registry.getSession(sessionId)).thenReturn(mockWsSession);
            when(mockWsSession.isOpen()).thenReturn(true);

            UserLogoutEvent event = new UserLogoutEvent(userId);
            stompEventListener.handleUserLogout(event);

            // then
            verify(redisTemplate).delete(String.format("ws:sub:%s:sub-1", sessionId));
            verify(mockWsSession).close();
            verify(sessionService).leave(contentId, userId);
        }

        @Test
        @DisplayName("웹소켓 close 도중 에러가 나도 catch 후 DB leave 처리를 완수한다.")
        void wsCloseError_continuesAndLeaves() throws Exception {
            // when
            when(setOperations.members(String.format("ws:user:%s:sessions", userId))).thenReturn(Set.of(sessionId));
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId))).thenReturn(Set.of(contentId.toString()));

            WebSocketSession mockWsSession = mock(WebSocketSession.class);
            when(registry.getSession(sessionId)).thenReturn(mockWsSession);
            when(mockWsSession.isOpen()).thenReturn(true);
            doThrow(new IOException()).when(mockWsSession).close();

            UserLogoutEvent event = new UserLogoutEvent(userId);
            stompEventListener.handleUserLogout(event);

            // then
            verify(sessionService).leave(contentId, userId);
        }

        @Test
        @DisplayName("leave 도중 DB 에러가 발생해도 catch 후 종료된다.")
        void leaveError_isCaught() {
            // when
            when(setOperations.members(String.format("ws:user:%s:sessions", userId))).thenReturn(Set.of(sessionId));
            when(setOperations.members(String.format("ws:session:%s:contents", sessionId))).thenReturn(Set.of(contentId.toString()));

            doThrow(new RuntimeException()).when(sessionService).leave(contentId, userId);

            UserLogoutEvent event = new UserLogoutEvent(userId);

            // then
            assertDoesNotThrow(() -> stompEventListener.handleUserLogout(event));
        }
    }
}
