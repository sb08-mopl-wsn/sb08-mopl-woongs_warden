package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.handler.LoginFailureHandler;
import com.mopl.mopl.global.exception.oauth2.LoginAttemptLockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginFailureHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final AuthenticationAttemptService authenticationAttemptService =
            mock(AuthenticationAttemptService.class);

    private final UserRepository userRepository = mock(UserRepository.class);

    private final JwtRegistry jwtRegistry = mock(JwtRegistry.class);

    private final LoginFailureHandler handler = new LoginFailureHandler(
            objectMapper,
            authenticationAttemptService,
            userRepository,
            jwtRegistry
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
        verifyNoInteractions(userRepository, jwtRegistry);
    }

    @Test
    @DisplayName("BadCredentialsException이고 실패 횟수 초과면 403과 LOGIN_LOCKED를 응답하고 JWT 정보를 무효화한다")
    void onAuthenticationFailure_badCredentials_lockedByAttempts() throws Exception {
        String email = "user@test.com";
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(authenticationAttemptService.recordLoginFailure(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        MockHttpServletResponse response =
                executeWithUsername(new BadCredentialsException("bad credentials"), email);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("LOGIN_LOCKED");
        assertThat(body.get("message").asText())
                .isEqualTo("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");

        verify(authenticationAttemptService).recordLoginFailure(email);
        verify(userRepository).findByEmail(email);
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("BadCredentialsException이고 실패 횟수 초과지만 사용자를 찾지 못하면 JWT 무효화를 하지 않는다")
    void onAuthenticationFailure_badCredentials_lockedButUserNotFound() throws Exception {
        String email = "missing@test.com";

        when(authenticationAttemptService.recordLoginFailure(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        MockHttpServletResponse response =
                executeWithUsername(new BadCredentialsException("bad credentials"), email);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("error").asText()).isEqualTo("LOGIN_LOCKED");
        assertThat(body.get("message").asText())
                .isEqualTo("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다.");

        verify(authenticationAttemptService).recordLoginFailure(email);
        verify(userRepository).findByEmail(email);
        verifyNoInteractions(jwtRegistry);
    }

    @Test
    @DisplayName("JWT 무효화 시 username 앞뒤 공백을 제거한 이메일로 사용자를 조회한다")
    void onAuthenticationFailure_lockedByAttempts_trimUsernameBeforeInvalidate() throws Exception {
        String rawEmail = "  user@test.com  ";
        String trimmedEmail = "user@test.com";
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(authenticationAttemptService.recordLoginFailure(rawEmail)).thenReturn(true);
        when(userRepository.findByEmail(trimmedEmail)).thenReturn(Optional.of(user));

        MockHttpServletResponse response =
                executeWithUsername(new BadCredentialsException("bad credentials"), rawEmail);

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("error").asText()).isEqualTo("LOGIN_LOCKED");

        verify(authenticationAttemptService).recordLoginFailure(rawEmail);
        verify(userRepository).findByEmail(trimmedEmail);
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
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
        verifyNoInteractions(userRepository, jwtRegistry);
    }

    @Test
    @DisplayName("LoginAttemptLockedException이면 403과 LOGIN_LOCKED를 응답한다")
    void onAuthenticationFailure_loginAttemptLockedException() throws Exception {
        MockHttpServletResponse response =
                execute(new LoginAttemptLockedException("로그인 시도가 일시적으로 제한되었습니다."));

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").asText()).isEqualTo("LOGIN_LOCKED");
        assertThat(body.get("message").asText()).isEqualTo("로그인 시도가 일시적으로 제한되었습니다.");

        verifyNoInteractions(authenticationAttemptService, userRepository, jwtRegistry);
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

        verifyNoInteractions(authenticationAttemptService, userRepository, jwtRegistry);
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

        verifyNoInteractions(authenticationAttemptService, userRepository, jwtRegistry);
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

        verifyNoInteractions(authenticationAttemptService, userRepository, jwtRegistry);
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

        verifyNoInteractions(authenticationAttemptService, userRepository, jwtRegistry);
    }

    private MockHttpServletResponse execute(AuthenticationException exception) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getCharacterEncoding()).isIn("UTF-8", "UTF8");

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
        assertThat(response.getCharacterEncoding()).isIn("UTF-8", "UTF8");

        return response;
    }
}