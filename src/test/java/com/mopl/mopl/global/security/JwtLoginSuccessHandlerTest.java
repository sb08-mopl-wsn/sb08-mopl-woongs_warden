package com.mopl.mopl.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.handler.JwtLoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtLoginSuccessHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final JwtRegistry jwtRegistry = mock(JwtRegistry.class);
    private final JwtLoginSuccessHandler handler = new JwtLoginSuccessHandler(
            objectMapper,
            tokenProvider,
            jwtRegistry
    );

    @Test
    @DisplayName("인증 성공 시 accessToken 응답, refresh 쿠키, JwtRegistry 등록을 수행한다")
    void onAuthenticationSuccess_IssuesTokenAndRegistersJwtInformation() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                "admin@admin.com",
                "관리자",
                null,
                Role.ADMIN,
                false
        );
        MoplUserDetails principal = new MoplUserDetails(userDto, "encoded-password", Collections.emptyMap());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateAccessToken(principal)).thenReturn("access.jwt.token");
        when(tokenProvider.generateRefreshToken(principal)).thenReturn("refresh.jwt.token");
        org.mockito.Mockito.doAnswer(invocation -> {
                    MockHttpServletResponse response = invocation.getArgument(0);
                    response.addHeader(
                            HttpHeaders.SET_COOKIE,
                            "REFRESH-TOKEN=refresh.jwt.token; Path=/; HttpOnly; SameSite=Lax"
                    );
                    return null;
                })
                .when(tokenProvider)
                .addRefreshCookie(any(), org.mockito.ArgumentMatchers.eq("refresh.jwt.token"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication
        );

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH-TOKEN=refresh.jwt.token");

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("accessToken").asText()).isEqualTo("access.jwt.token");
        assertThat(body.get("userDto").get("id").asText()).isEqualTo(userId.toString());
        assertThat(body.get("userDto").get("email").asText()).isEqualTo("admin@admin.com");
        assertThat(body.get("userDto").get("name").asText()).isEqualTo("관리자");

        ArgumentCaptor<JwtInformation> captor = ArgumentCaptor.forClass(JwtInformation.class);
        verify(jwtRegistry).registerJwtInformation(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(userDto);
        assertThat(captor.getValue().getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("refresh.jwt.token");
    }

    @Test
    @DisplayName("principal이 MoplUserDetails가 아니면 401을 반환한다")
    void onAuthenticationSuccess_InvalidPrincipal_ReturnsUnauthorized() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not-user-details");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication
        );

        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asText()).isEqualTo("Invalid principal");
        verifyNoInteractions(jwtRegistry);
    }

    @Test
    @DisplayName("토큰 발급 중 예외가 발생하면 500을 반환한다")
    void onAuthenticationSuccess_TokenGenerationFails_ReturnsInternalServerError() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-05-08T00:00:00Z"),
                "user@test.com",
                "일반유저",
                null,
                Role.USER,
                false
        );
        MoplUserDetails principal = new MoplUserDetails(userDto, "encoded-password", Collections.emptyMap());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateAccessToken(principal))
                .thenThrow(new com.nimbusds.jose.JOSEException("sign failed"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                mock(HttpServletRequest.class),
                response,
                authentication
        );

        assertThat(response.getStatus()).isEqualTo(500);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asText()).isEqualTo("Token generation failed");
        verifyNoInteractions(jwtRegistry);
    }
}
