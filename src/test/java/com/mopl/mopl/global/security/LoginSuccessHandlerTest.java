package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.handler.LoginSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

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

        Authentication authentication = mock(Authentication.class);
        MoplUserDetails userDetails = mock(MoplUserDetails.class);
        UserDto userDto = mock(UserDto.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUserDto()).thenReturn(userDto);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentAsString()).isEqualTo(objectMapper.writeValueAsString(userDto));
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