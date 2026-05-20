package com.mopl.mopl.global.security;

import com.mopl.mopl.global.auth.handler.OAuth2LoginFailureHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OAuth2LoginFailureHandlerTest {

    private OAuth2LoginFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2LoginFailureHandler();
        ReflectionTestUtils.setField(
                failureHandler,
                "failureRedirectUrl",
                "http://localhost:8080/sign-in?error=oauth2"
        );
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 시 failureRedirectUrl로 redirect한다")
    void onAuthenticationFailure_redirectFailureUrl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = mock(AuthenticationException.class);

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:8080/sign-in?error=oauth2");
    }
}