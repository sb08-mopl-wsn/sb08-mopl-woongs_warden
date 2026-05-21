package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.auth.handler.CustomSessionExpiredStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.session.SessionInformationExpiredEvent;

import static org.assertj.core.api.Assertions.assertThat;

class CustomSessionExpiredStrategyTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CustomSessionExpiredStrategy strategy = new CustomSessionExpiredStrategy();

    @Test
    @DisplayName("세션 만료 감지 시 401과 SESSION_EXPIRED 응답을 반환한다")
    void onExpiredSessionDetected_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        SessionInformation sessionInformation =
                new SessionInformation("principal", "session-id", new java.util.Date());

        SessionInformationExpiredEvent event =
                new SessionInformationExpiredEvent(sessionInformation, request, response);

        strategy.onExpiredSessionDetected(event);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asText()).isEqualTo("SESSION_EXPIRED");
        assertThat(body.get("message").asText())
                .isEqualTo("다른 곳에서 로그인 되어 현 세션이 만료되었습니다. 다시 로그인해주세요.");
    }
}