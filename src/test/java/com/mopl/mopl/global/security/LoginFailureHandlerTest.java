package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
import com.mopl.mopl.global.auth.handler.LoginFailureHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticationAttemptService authenticationAttemptService =
            mock(AuthenticationAttemptService.class);

    private final LoginFailureHandler handler = new LoginFailureHandler(
            objectMapper,
            authenticationAttemptService
    );

    @Test
    @DisplayName("BadCredentialsException이면 401과 AUTHENTICATION_FAILED를 응답한다")
    void onAuthenticationFailure_badCredentials() throws Exception {
        String email = "user@test.com";

        when(authenticationAttemptService.recordLoginFailure(email)).thenReturn(false);

        MockHttpServletResponse response =
                executeWithUsername(new BadCredentialsException("bad credentials"), email);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.get("message").asText()).isEqualTo("ID/PW가 올바르지 않습니다.");

        verify(authenticationAttemptService).recordLoginFailure(email);
    }

    @Test
    @DisplayName("BadCredentialsException이고 실패 횟수 초과면 403과 LOGIN_LOCKED를 응답한다")
    void onAuthenticationFailure_badCredentials_lockedByAttempts() throws Exception {
        String email = "user@test.com";

        when(authenticationAttemptService.recordLoginFailure(email)).thenReturn(true);

        MockHttpServletResponse response =
                executeWithUsername(new BadCredentialsException("bad credentials"), email);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("LOGIN_LOCKED");
        assertThat(body.get("message").asText())
                .isEqualTo("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");

        verify(authenticationAttemptService).recordLoginFailure(email);
    }

    @Test
    @DisplayName("username 파라미터가 없으면 email 파라미터로 로그인 실패 횟수를 기록한다")
    void onAuthenticationFailure_badCredentials_useEmailParameter() throws Exception {
        String email = "email-param@test.com";

        when(authenticationAttemptService.recordLoginFailure(email)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("email", email);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("bad credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.get("message").asText()).isEqualTo("ID/PW가 올바르지 않습니다.");

        verify(authenticationAttemptService).recordLoginFailure(email);
    }

    @Test
    @DisplayName("LockedException이면 403과 ACCOUNT_LOCKED를 응답한다")
    void onAuthenticationFailure_locked() throws Exception {
        MockHttpServletResponse response =
                execute(new LockedException("잠긴 계정입니다. 관리자에게 문의하세요."));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(body.get("message").asText()).isEqualTo("잠긴 계정입니다. 관리자에게 문의하세요.");

        verifyNoInteractions(authenticationAttemptService);
    }

    @Test
    @DisplayName("LockedException 메시지가 비어 있으면 기본 잠금 메시지를 응답한다")
    void onAuthenticationFailure_locked_blankMessage() throws Exception {
        MockHttpServletResponse response =
                execute(new LockedException(""));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(body.get("message").asText()).isEqualTo("잠긴 계정입니다. 관리자에게 문의하세요.");

        verifyNoInteractions(authenticationAttemptService);
    }

    @Test
    @DisplayName("DisabledException이면 403과 AUTHENTICATION_FAILED를 응답한다")
    void onAuthenticationFailure_disabled() throws Exception {
        MockHttpServletResponse response = execute(new DisabledException("disabled"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.get("message").asText()).isEqualTo("비활성 계정입니다.");

        verifyNoInteractions(authenticationAttemptService);
    }

    @Test
    @DisplayName("그 외 AuthenticationException이면 401과 기본 로그인 실패 메시지를 응답한다")
    void onAuthenticationFailure_others() throws Exception {
        MockHttpServletResponse response = execute(new AuthenticationException("unknown") {});

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.get("message").asText()).isEqualTo("로그인 실패 입니다.");

        verifyNoInteractions(authenticationAttemptService);
    }

    private MockHttpServletResponse execute(AuthenticationException exception) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        return response;
    }

    private MockHttpServletResponse executeWithUsername(
            AuthenticationException exception,
            String username
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", username);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        return response;
    }
}