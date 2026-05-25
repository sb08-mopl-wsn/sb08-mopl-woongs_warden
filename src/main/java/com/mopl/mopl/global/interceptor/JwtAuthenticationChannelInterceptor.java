package com.mopl.mopl.global.interceptor;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.event.kafka.UserSecurityEventKafkaPublisher;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final RoleHierarchy roleHierarchy;
    private final JwtRegistry jwtRegistry;
    private final UserSecurityEventKafkaPublisher kafkaPublisher;

    private static final String PREFIX = "Bearer ";
    private static final String HEADER_NAME = "ACCESS_TOKEN";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleAuthentication(accessor);
        }

        return message;
    }

    private void handleAuthentication(StompHeaderAccessor accessor) {
        log.info("[WebSocket] CONNECT 수신");  // 추가
        String token = resolveToken(accessor)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.UNAUTHORIZED));
        log.info("[WebSocket] 토큰 추출 성공");

        // HTTP 필터와 동일한 로직: 토큰 검증 + JWT 세션 확인
        if (tokenProvider.validateAccessToken(token)
                && jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {

            MoplUserDetails userDetails = tokenProvider.parseAccessToken(token);

            if (userDetails.getUserDto() == null) {
                log.error("[WebSocket] 토큰 파싱은 성공했으나 UserDto가 null입니다.");
                throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
            }

            UserDto userDto = userDetails.getUserDto();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            roleHierarchy.getReachableGrantedAuthorities(
                                    userDetails.getAuthorities()
                            )
                    );

            accessor.setUser(authentication);
            kafkaPublisher.publishSecurityEvent("WS_AUTH_SUCCESS", userDto.id(), userDto.email(), "웹소켓 CONNECT 인증 성공");
            log.debug("웹 소켓 유저를 위한 인증 설정 완료. user: {}", userDto.name());
        } else {
            kafkaPublisher.publishSecurityEvent("WS_AUTH_FAILED", null, null, "웹소켓 CONNECT 인증 실패");
            log.debug("웹소켓 통신을 위한 유효하지 않은 JWT 토큰");
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
    }

    // STOMP 헤더에서 Access Token을 추출
    private Optional<String> resolveToken(StompHeaderAccessor accessor) {
        String prefix = PREFIX;

        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(prefix)) {
            return Optional.of(authHeader.substring(prefix.length()));
        }

        String accessTokenHeader = accessor.getFirstNativeHeader(HEADER_NAME);
        if (StringUtils.hasText(accessTokenHeader)) {
            return Optional.of(accessTokenHeader);
        }

        return Optional.empty();
    }
}
