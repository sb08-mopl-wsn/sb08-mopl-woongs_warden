package com.mopl.mopl.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.component.WebSocketSessionRegistry;
import com.mopl.mopl.global.interceptor.JwtAuthenticationChannelInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private final JwtAuthenticationChannelInterceptor jwtAuthenticationChannelInterceptor;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        messageConverters.add(converter);
        return false;
    }

    private AuthorizationChannelInterceptor authorizationChannelInterceptor() {
        return new AuthorizationChannelInterceptor(
                MessageMatcherDelegatingAuthorizationManager.builder()
                        .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll()
                        .anyMessage().hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .build()
        );
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        log.info("[WebSocket 설정] 메시지 브로커 설정 시작");

        // [구독] 클라이언트가 메시지를 받을 때 사용하는 경로
        config.enableSimpleBroker("/sub");

        // [발행] 클라이언트가 메시지를 보낼 때 사용하는 경로
        config.setApplicationDestinationPrefixes("/pub");

        log.info("[WebSocket 설정] 메시지 브로커 설정 완료");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        log.info("[WebSocket 설정] STOMP 엔드포인트 등록 시작");

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(3000)
                .setDisconnectDelay(5000);

        log.info("[WebSocket 설정] STOMP 엔드포인트 등록 완료");
        log.debug("[STOMP 엔드포인트] 경로: /ws, SockJS 폴백: 활성화, CORS: 모든 오리진 허용");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                jwtAuthenticationChannelInterceptor, // 1순위 - 인증
                new SecurityContextChannelInterceptor(), // 2순위
                authorizationChannelInterceptor() // 3순위 - 인가
        );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    // 웹소켓 연결을 연결/끊기 위한 로직
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessionRegistry.addSession(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessionRegistry.removeSession(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        });
    }
}
