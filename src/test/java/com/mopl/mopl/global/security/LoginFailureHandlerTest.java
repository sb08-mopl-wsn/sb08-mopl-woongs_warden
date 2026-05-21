package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class LoginFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final LoginFailureHandler handler = new LoginFailureHandler(objectMapper);

    @Test
    @DisplayName("BadCredentialsException이면 401과 AUTHENTICATION_FAILED를 응답한다")
    void onAuthenticationFailure_badCredentials() throws Exception {
        MockHttpServletResponse response = execute(new BadCredentialsException("bad credentials"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.get("message").asText()).isEqualTo("ID/PW가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("LockedException이면 403과 ACCOUNT_LOCKED를 응답한다")
    void onAuthenticationFailure_locked() throws Exception {
        MockHttpServletResponse response = execute(new LockedException("locked"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(body.get("message").asText()).isEqualTo("잠긴 계정입니다. 관리자에게 문의하세요.");
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
    }

    private MockHttpServletResponse execute(AuthenticationException exception) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        return response;
    }
}