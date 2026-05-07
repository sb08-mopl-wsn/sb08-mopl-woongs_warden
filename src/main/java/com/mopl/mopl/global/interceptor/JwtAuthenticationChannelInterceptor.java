package com.mopl.mopl.global.interceptor;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
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

    private static final String PREFIX = "Bearer ";
    private static final String HEADER_NAME = "ACCESS_TOKEN";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );
        // TODO: 커스텀 예외 처리 필
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor)
                    .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));

            // HTTP 필터와 동일한 로직: 토큰 검증 + JWT 세션 확인
            if (tokenProvider.validateAccessToken(token)
                    && jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {

                MoplUserDetails userDetails = tokenProvider.parseAccessToken(token);
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
                log.debug("웹 소켓 유저를 위한 인증 설정 완료. user: {}", userDto.name());
            } else {
                // TODO: 커스텀 예외 처리 필
                log.debug("웹소켓 통신을 위한 유효하지 않은 JWT 토큰");
                throw new RuntimeException("INVALID_TOKEN");
            }
        }

        return message;
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
