package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.auth.exception.AuthErrorCode;
import com.mopl.mopl.global.auth.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final CustomAuthenticationEntryPoint entryPoint =
            new CustomAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("인증되지 않은 요청이면 401과 AUTH-UNAUTHORIZED 에러 응답을 반환한다")
    void commence_returnsUnauthorizedErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException exception = new AuthenticationException("unauthorized") {
        };

        entryPoint.commence(request, response, exception);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        assertThat(body.toString()).contains(AuthErrorCode.AUTH_UNAUTHORIZED.getCode());
        assertThat(body.toString()).contains("인증이 필요합니다.");
    }
}