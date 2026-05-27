package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.handler.JwtLogoutHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.mockito.Mockito.*;

class JwtLogoutHandlerTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final JwtRegistry jwtRegistry = mock(JwtRegistry.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JwtLogoutHandler handler = new JwtLogoutHandler(tokenProvider, jwtRegistry, eventPublisher);

    @Test
    @DisplayName("인증 객체에서 사용자 ID를 찾으면 해당 사용자의 JWT 정보를 무효화한다")
    void logout_withAuthentication_invalidatesUserTokens() {
        UUID userId = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication authentication = mock(Authentication.class);
        MoplUserDetails userDetails = mock(MoplUserDetails.class);
        UserDto userDto = mock(UserDto.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUserDto()).thenReturn(userDto);
        when(userDto.id()).thenReturn(userId);

        handler.logout(request, response, authentication);

        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(tokenProvider).expireRefreshCookie(response);
    }

    @Test
    @DisplayName("Authentication이 없어도 Authorization 헤더의 Access Token에서 사용자 ID를 찾으면 JWT 정보를 무효화한다")
    void logout_withAuthorizationHeader_invalidatesUserTokens() {
        UUID userId = UUID.randomUUID();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer access-token");

        when(tokenProvider.validateRefreshToken("access-token")).thenReturn(false);
        when(tokenProvider.validateAccessToken("access-token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("access-token")).thenReturn(userId);

        handler.logout(request, response, null);

        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        verify(tokenProvider).expireRefreshCookie(response);
    }

    @Test
    @DisplayName("사용자 ID를 찾지 못해도 Refresh Cookie 삭제 처리는 수행한다")
    void logout_withoutUserId_onlyExpiresRefreshCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.logout(request, response, null);

        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
        verify(tokenProvider).expireRefreshCookie(response);
    }

    @Test
    @DisplayName("Authorization 헤더 파싱 중 예외가 발생해도 로그아웃 처리는 계속된다")
    void logout_whenTokenParsingFails_doesNotThrow() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer invalid-token");

        when(tokenProvider.validateRefreshToken("invalid-token"))
                .thenThrow(new RuntimeException("token error"));

        handler.logout(request, response, null);

        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
        verify(tokenProvider).expireRefreshCookie(response);
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 있어도 파싱 실패 시 예외를 전파하지 않는다")
    void logout_withInvalidRefreshCookie_doesNotThrow() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setCookies(new Cookie(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME, "invalid-refresh-token"));

        when(tokenProvider.validateRefreshToken("invalid-refresh-token"))
                .thenThrow(new RuntimeException("token error"));

        handler.logout(request, response, null);

        verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
        verify(tokenProvider).expireRefreshCookie(response);
    }
}