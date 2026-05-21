package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.auth.handler.CustomAccessDeniedHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CustomAccessDeniedHandler handler =
            new CustomAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("접근 권한이 없으면 403과 ACCESS_DENIED 응답을 반환한다")
    void handle_returnsForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("access denied"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(body.get("message").asText()).isEqualTo("해당 리소스 접근 권한이 없습니다.");
        assertThat(body.get("status").asInt()).isEqualTo(403);
    }
}