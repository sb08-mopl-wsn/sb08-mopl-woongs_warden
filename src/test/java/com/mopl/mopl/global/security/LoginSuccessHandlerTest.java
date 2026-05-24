package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.handler.LoginSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginSuccessHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final LoginSuccessHandler handler = new LoginSuccessHandler(objectMapper);

    @Test
    @DisplayName("로그인 성공 시 사용자 정보를 JSON으로 응답한다")
    void onAuthenticationSuccess_withMoplUserDetails_writesUserJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                "user@test.com",
                "일반유저",
                null,
                Role.USER,
                false
        );

        Authentication authentication = mock(Authentication.class);
        MoplUserDetails userDetails = new MoplUserDetails(userDto, "encoded-password", Collections.emptyMap());

        when(authentication.getPrincipal()).thenReturn(userDetails);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(body.get("id").asText()).isEqualTo(userId.toString());
        assertThat(body.get("email").asText()).isEqualTo("user@test.com");
        assertThat(body.get("name").asText()).isEqualTo("일반유저");
        assertThat(body.get("role").asText()).isEqualTo("USER");
        assertThat(body.get("locked").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("principal이 MoplUserDetails가 아니면 500 응답을 반환한다")
    void onAuthenticationSuccess_withInvalidPrincipal_returnsInternalServerError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("invalid-principal");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("error");
    }
}