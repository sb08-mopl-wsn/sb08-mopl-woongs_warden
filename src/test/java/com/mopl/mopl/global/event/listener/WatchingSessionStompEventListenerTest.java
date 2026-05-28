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
import java.time.Instant;
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
        @DisplayName("인증된 사용자가 연결되면 예외 없이 유저-세션 매핑이 등록된다.")
        void authenticatedUser_registersSessionUserMapping() {
            Message<byte[]> message = createStompMessage(StompCommand.CONNECT, null);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            assertDoesNotThrow(() -> stompEventListener.handleConnect(event));
        }

        @Test
        @DisplayName("인증 정보가 없는 연결은 내부 예외를 삼키고 정상 종료된다.")
        void unauthenticatedUser_ignored() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId(sessionId);
            accessor.setUser(null);
            accessor.setLeaveMutable(true);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            assertDoesNotThrow(() -> stompEventListener.handleConnect(event));
        }
    }

    @Nested
    @DisplayName("handleSubscribe()")
    class HandleSubscribe {

        @Test
        @DisplayName("올바른 watch 경로를 구독하면 세션 정보가 기록되고 서비스의 join()이 호출된다.")
        void validDestination_callsWatchingSessionJoin() {
            // given
            String validDestination = "/sub/contents/" + contentId + "/watch";
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, validDestination);
            SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

            // when
            stompEventListener.handleSubscribe(event);

            // then
            verify(sessionService, times(1)).join(contentId, userId);
        }

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
        }

        @Test
        @DisplayName("같은 유저가 동일한 콘텐츠를 이미 시청 중이면 join()을 중복 호출하지 않는다.")
        void alreadyWatching_doesNotCallJoinAgain() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            subscriptionId = "another-sub-id-5678";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            verify(sessionService, times(1)).join(contentId, userId);
        }

        @Test
        @DisplayName("join() 호출 중 BusinessException이 발생하면 로그를 남기고 정상 종료된다.")
        void joinThrowsBusinessException_catchesAndLogsWarning() {
            // given
            String validDestination = "/sub/contents/" + contentId + "/watch";
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, validDestination);
            SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

            doThrow(new BusinessException((GlobalErrorCode.INVALID_INPUT)))
                    .when(sessionService).join(contentId, userId);

            // when
            stompEventListener.handleSubscribe(event);

            // then
            verify(sessionService, times(1)).join(contentId, userId);
        }

        @Test
        @DisplayName("join() 호출 중 일반 Exception이 발생하면 로그를 남기고 정상 종료된다.")
        void joinThrowsGenericException_catchesAndLogsError() {
            // given
            String validDestination = "/sub/contents/" + contentId + "/watch";
            Message<byte[]> message = createStompMessage(StompCommand.SUBSCRIBE, validDestination);
            SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

            doThrow(new RuntimeException("DB 연결 오류"))
                    .when(sessionService).join(contentId, userId);

            // when
            stompEventListener.handleSubscribe(event);

            // then
            verify(sessionService, times(1)).join(contentId, userId);
        }
    }

    @Nested
    @DisplayName("handleUnSubscribe()")
    class HandleUnSubscribe {

        @Test
        @DisplayName("기존에 정상 구독 정보가 등록되어 있던 세션이 구독 취소되면 서비스의 leave()를 호출한다.")
        void existingSubscription_callsWatchingSessionLeave() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            Message<byte[]> subMessage = createStompMessage(StompCommand.SUBSCRIBE, destination);
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, subMessage));

            Message<byte[]> unsubMessage = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, unsubMessage);

            // when
            stompEventListener.handleUnSubscribe(event);

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("내부 맵에 매핑된 기록이 없는 익명의 구독 취소 요청이 오면 무시한다.")
        void nonExistentSubscription_doesNothing() {
            // given
            Message<byte[]> unsubMessage = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, unsubMessage);

            // when
            stompEventListener.handleUnSubscribe(event);

            // then
            verifyNoInteractions(sessionService);
        }

        @Test
        @DisplayName("leave() 호출 중 BusinessException이 발생하면 catch 블록이 실행된다.")
        void leaveThrowsBusinessException_catchesAndLogsWarning() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            Message<byte[]> unsubMessage = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, unsubMessage);

            doThrow(new BusinessException(GlobalErrorCode.INVALID_INPUT))
                    .when(sessionService).leave(contentId, userId);

            // when
            stompEventListener.handleUnSubscribe(event);

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("leave() 호출 중 일반 Exception이 발생하면 catch 블록이 실행된다.")
        void leaveThrowsGenericException_catchesAndLogsError() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            Message<byte[]> unsubMessage = createStompMessage(StompCommand.UNSUBSCRIBE, null);
            SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, unsubMessage);

            doThrow(new RuntimeException())
                    .when(sessionService).leave(contentId, userId);

            // when
            stompEventListener.handleUnSubscribe(event);

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("구독 취소 전 동일 콘텐츠에 재구독이 이미 완료된 경우, 빠른 재구독을 감지하고 leave()를 호출하지 않는다.")
        void quickResubscribeSameContent_doesNotCallLeave() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            String firstSubId = subscriptionId;

            // 최초 구독
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            // 구독 취소 전 새 subscriptionId로 동일 콘텐츠 재구독
            subscriptionId = "new-sub-id-5678";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            // when
            // 첫 번째 구독 ID로 구독 취소, 새 구독이 남아있으므로 isSessionStillSubscribed = true
            subscriptionId = firstSubId;
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, createStompMessage(StompCommand.UNSUBSCRIBE, null)));

            // then
            verify(sessionService, times(1)).join(contentId, userId);
            verify(sessionService, never()).leave(any(), any());
        }

        @Test
        @DisplayName("하나의 세션에서 여러 콘텐츠 구독 중 하나만 취소하면 나머지 콘텐츠 시청은 유지된다.")
        void unsubscribeOneOfMultipleContents_remainingContentStillActive() {
            // given
            UUID contentId2 = UUID.randomUUID();
            String firstSubId = subscriptionId;

            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch")));
            subscriptionId = "sub-id-for-content2";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId2 + "/watch")));

            // when
            // contentId만 구독 취소
            subscriptionId = firstSubId;
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, createStompMessage(StompCommand.UNSUBSCRIBE, null)));

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
            verify(sessionService, never()).leave(contentId2, userId);
        }
    }

    @Nested
    @DisplayName("handleLeave()")
    class HandleLeave {

        @Test
        @DisplayName("유저의 웹소켓이 완전히 단절되면 맵에서 보관중이던 모든 콘텐츠 시청 세션을 해제하며 leave()가 호출된다.")
        void webSocketDisconnect_clearsAllMappedSessionsAndCallsLeave() {
            UUID extraContentId = UUID.randomUUID();

            Message<byte[]> subMessage1 = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, subMessage1));

            subscriptionId = "another-sub-1234";
            Message<byte[]> subMessage2 = createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + extraContentId + "/watch");
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, subMessage2));

            Message<byte[]> disconnectMessage = createStompMessage(StompCommand.DISCONNECT, null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, disconnectMessage, sessionId, null);

            // when
            stompEventListener.handleLeave(event);

            verify(sessionService, times(1)).leave(contentId, userId);
            verify(sessionService, times(1)).leave(extraContentId, userId);
        }

        @Test
        @DisplayName("이미 구독 취소를 마치고 나간 세션이 끊어질 때는 중복 leave()를 트리거하지 않는다.")
        void disconnectAfterUnsubscribe_skipsDuplicateLeave() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));
            stompEventListener.handleUnSubscribe(new SessionUnsubscribeEvent(this, createStompMessage(StompCommand.UNSUBSCRIBE, null)));

            SessionDisconnectEvent event = new SessionDisconnectEvent(this, createStompMessage(StompCommand.DISCONNECT, null), sessionId, null);

            // when
            stompEventListener.handleLeave(event);

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("연속 퇴장 처리 루프 중 특정 방에서 예외가 발생하더라도, 루프가 멈추지 않고 다른 방의 leave()까지 정상 호출한다.")
        void disconnectLoopThrowsException_continuesAndCallsNextLeave() {
            UUID extraContentId = UUID.randomUUID();

            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch")));

            subscriptionId = "another-sub-1234";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + extraContentId + "/watch")));

            doThrow(new NullPointerException())
                    .when(sessionService).leave(contentId, userId);

            SessionDisconnectEvent event = new SessionDisconnectEvent(this, createStompMessage(StompCommand.DISCONNECT, null), sessionId, null);

            // when
            stompEventListener.handleLeave(event);

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
            verify(sessionService, times(1)).leave(extraContentId, userId);
        }

        @Test
        @DisplayName("extractUserId 단계에서 예외가 발생하면 가장 외곽의 catch 블록에서 에러 로그를 남긴다.")
        void extractUserIdThrowsException_catchesInOuterBlock() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            accessor.setSessionId(sessionId);
            accessor.setUser(null);
            accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, sessionId);
            accessor.setLeaveMutable(true);

            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, null);

            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch")));

            // when
            stompEventListener.handleLeave(event);

            // then
            verify(sessionService, never()).leave(any(), any());
        }

        @Test
        @DisplayName("구독 없이 연결만 했다가 끊어진 경우 contentIds가 null이므로 leave()를 호출하지 않는다.")
        void disconnectWithoutSubscription_contentIdsNull_doesNotCallLeave() {
            // given
            stompEventListener.handleConnect(new SessionConnectedEvent(this, createStompMessage(StompCommand.CONNECT, null)));

            SessionDisconnectEvent event = new SessionDisconnectEvent(this, createStompMessage(StompCommand.DISCONNECT, null), sessionId, null);

            // when
            stompEventListener.handleLeave(event);

            // then
            verifyNoInteractions(sessionService);
        }

        @Test
        @DisplayName("멀티 세션 환경에서 한 세션이 끊어져도 다른 세션이 동일 콘텐츠를 시청 중이면 leave()를 호출하지 않는다.")
        void multiSession_oneDisconnects_otherSessionStillWatching_doesNotCallLeave() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            String session1 = sessionId;

            // 세션 1 구독
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            // 세션 2 구독
            sessionId = "mock-session-id-5678";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            // 세션 1 연결 종료
            sessionId = session1;
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, createStompMessage(StompCommand.DISCONNECT, null), session1, null);

            // when
            stompEventListener.handleLeave(event);

            // then
            verify(sessionService, never()).leave(contentId, userId);
        }
    }

    @Nested
    @DisplayName("handleUserLogout()")
    class HandleUserLogout {

        @Test
        @DisplayName("유저 세션 맵에 없는 유저의 로그아웃 요청은 early return하고 아무것도 수행하지 않는다.")
        void userNotInSessionMap_earlyReturn_doesNothing() {
            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verifyNoInteractions(sessionService);
        }

        @Test
        @DisplayName("로그아웃 시 열려있는 웹소켓 세션을 강제 종료하고 모든 콘텐츠에 대해 leave()를 호출한다.")
        void logout_openSession_closesAndCallsLeave() throws Exception {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            WebSocketSession mockWsSession = mock(WebSocketSession.class);
            when(registry.getSession(sessionId)).thenReturn(mockWsSession);
            when(mockWsSession.isOpen()).thenReturn(true);

            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verify(mockWsSession).close();
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("로그아웃 시 웹소켓 세션이 null이면 close()를 호출하지 않고 leave()는 정상 실행된다.")
        void logout_wsSessionNull_skipsClose_callsLeave() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            when(registry.getSession(sessionId)).thenReturn(null);

            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("로그아웃 시 웹소켓 세션이 이미 닫혀있으면 close()를 호출하지 않는다.")
        void logout_wsSessionNotOpen_skipsClose() throws Exception {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            WebSocketSession mockWsSession = mock(WebSocketSession.class);
            when(registry.getSession(sessionId)).thenReturn(mockWsSession);
            when(mockWsSession.isOpen()).thenReturn(false);

            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verify(mockWsSession, never()).close();
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("로그아웃 시 웹소켓 close()에서 예외가 발생해도 catch하고 leave()까지 정상 실행된다.")
        void logout_wsCloseThrowsException_continuesAndCallsLeave() throws Exception {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            WebSocketSession mockWsSession = mock(WebSocketSession.class);
            when(registry.getSession(sessionId)).thenReturn(mockWsSession);
            when(mockWsSession.isOpen()).thenReturn(true);
            doThrow(new IOException("강제 종료 실패")).when(mockWsSession).close();

            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("로그아웃 시 leave()에서 예외가 발생해도 catch하고 정상 종료된다.")
        void logout_leaveThrowsException_catchesAndContinues() {
            // given
            String destination = "/sub/contents/" + contentId + "/watch";
            stompEventListener.handleSubscribe(new SessionSubscribeEvent(this, createStompMessage(StompCommand.SUBSCRIBE, destination)));

            when(registry.getSession(sessionId)).thenReturn(null);
            doThrow(new RuntimeException("DB 오류")).when(sessionService).leave(contentId, userId);

            // when & then
            assertDoesNotThrow(() -> stompEventListener.handleUserLogout(mockLogoutEvent()));
            verify(sessionService, times(1)).leave(contentId, userId);
        }

        @Test
        @DisplayName("구독 없이 연결만 한 유저가 로그아웃하면 contentIds가 null이어도 예외 없이 정상 종료된다.")
        void logout_noSubscriptions_contentIdsNull_doesNotCallLeave() {
            // handleConnect()로 userSessionMap에만 세션 등록
            // given
            stompEventListener.handleConnect(new SessionConnectedEvent(this, createStompMessage(StompCommand.CONNECT, null)));
            when(registry.getSession(sessionId)).thenReturn(null);

            // when
            stompEventListener.handleUserLogout(mockLogoutEvent());

            // then
            verifyNoInteractions(sessionService);
        }
    }
}
