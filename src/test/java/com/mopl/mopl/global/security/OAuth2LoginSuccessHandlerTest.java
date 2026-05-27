package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.handler.OAuth2LoginSuccessHandler;
import com.mopl.mopl.global.exception.oauth2.OAuth2PrincipalException;
import com.mopl.mopl.global.exception.oauth2.OAuth2FailedTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtRegistry jwtRegistry;

    @Mock
    private Authentication authentication;

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(jwtTokenProvider, jwtRegistry);
        ReflectionTestUtils.setField(
                successHandler,
                "successRedirectUrl",
                "http://localhost:8080/"
        );
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 시 JWT를 생성하고 refreshToken 쿠키를 추가한 뒤 redirect한다")
    void onAuthenticationSuccess_success() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.now(),
                "google@test.com",
                "구글사용자",
                null,
                Role.USER,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(userDto, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(authentication.getPrincipal()).willReturn(userDetails);
        given(jwtTokenProvider.generateAccessToken(userDetails)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(userDetails)).willReturn("refresh-token");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtTokenProvider).generateAccessToken(userDetails);
        verify(jwtTokenProvider).generateRefreshToken(userDetails);
        verify(jwtRegistry).registerJwtInformation(any());
        verify(jwtTokenProvider).addRefreshCookie(response, "refresh-token");

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080/");
    }

    @Test
    @DisplayName("principal이 MoplUserDetails가 아니면 OAuth2PrincipalException이 발생한다")
    void onAuthenticationSuccess_invalidPrincipal_throwOAuth2PrincipalException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(authentication.getPrincipal()).willReturn("invalid-principal");

        assertThatThrownBy(() ->
                successHandler.onAuthenticationSuccess(request, response, authentication)
        ).isInstanceOf(OAuth2PrincipalException.class);

        verifyNoInteractions(jwtTokenProvider, jwtRegistry);
    }

    @Test
    @DisplayName("토큰 생성 중 예외가 발생하면 Oauth2FailedTokenException이 발생한다")
    void onAuthenticationSuccess_tokenGenerationFailed_throwOauth2FailedTokenException() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.now(),
                "google@test.com",
                "구글사용자",
                null,
                Role.USER,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(userDto, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(authentication.getPrincipal()).willReturn(userDetails);
        given(jwtTokenProvider.generateAccessToken(userDetails))
                .willThrow(new RuntimeException("token error"));

        assertThatThrownBy(() ->
                successHandler.onAuthenticationSuccess(request, response, authentication)
        ).isInstanceOf(OAuth2FailedTokenException.class);
    }
}