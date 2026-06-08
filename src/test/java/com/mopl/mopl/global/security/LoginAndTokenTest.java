package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import com.mopl.mopl.global.auth.handler.JwtLoginSuccessHandler;
import com.mopl.mopl.global.auth.handler.LoginFailureHandler;
import com.mopl.mopl.global.exception.oauth2.LoginAttemptLockedException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        LoginAndTokenTest.TestSecurityConfig.class,
        JwtLoginSuccessHandler.class,
        LoginFailureHandler.class
})
class LoginAndTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private AuthenticationAttemptService authenticationAttemptService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("/api/auth/sign-in 로그인 성공 시 Access Token 응답과 Refresh Token 쿠키를 발급한다")
    void signInSuccess_IssuesTokens() throws Exception {
        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                "admin@admin.com",
                "관리자",
                null,
                Role.ADMIN,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(
                userDto,
                passwordEncoder.encode("Admin1234!")
        );

        given(userDetailsService.loadUserByUsername("admin@admin.com"))
                .willReturn(userDetails);
        given(jwtTokenProvider.generateAccessToken(userDetails))
                .willReturn("access.jwt.token");
        given(jwtTokenProvider.generateRefreshToken(userDetails, false))
                .willReturn("refresh.jwt.token");

        org.mockito.Mockito.doAnswer(invocation -> {
                    HttpServletResponse response = invocation.getArgument(0);
                    response.addHeader(
                            HttpHeaders.SET_COOKIE,
                            "REFRESH_TOKEN=refresh.jwt.token; Path=/; HttpOnly; SameSite=Lax"
                    );
                    return null;
                })
                .when(jwtTokenProvider)
                .addRefreshCookie(
                        any(HttpServletResponse.class),
                        eq("refresh.jwt.token"),
                        eq(false)
                );

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@admin.com")
                        .param("password", "Admin1234!"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("REFRESH_TOKEN=refresh.jwt.token")
                ))
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.userDto.id").value(userId.toString()))
                .andExpect(jsonPath("$.userDto.email").value("admin@admin.com"))
                .andExpect(jsonPath("$.userDto.name").value("관리자"))
                .andExpect(jsonPath("$.userDto.role").value("ADMIN"));

        ArgumentCaptor<JwtInformation> jwtInformationCaptor =
                ArgumentCaptor.forClass(JwtInformation.class);

        verify(authenticationAttemptService).resetLoginFailures("admin@admin.com");
        verify(jwtRegistry).registerJwtInformation(jwtInformationCaptor.capture());

        JwtInformation saved = jwtInformationCaptor.getValue();
        assertThat(saved.getUser().email()).isEqualTo("admin@admin.com");
        assertThat(saved.getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(saved.getRefreshToken()).isEqualTo("refresh.jwt.token");
    }

    @Test
    @DisplayName("/api/auth/sign-in 로그인 성공 - rememberMe=true이면 rememberMe Refresh Token 쿠키를 발급한다")
    void signInSuccess_RememberMe_IssuesRememberMeRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();

        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                "admin@admin.com",
                "관리자",
                null,
                Role.ADMIN,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(
                userDto,
                passwordEncoder.encode("Admin1234!")
        );

        given(userDetailsService.loadUserByUsername("admin@admin.com"))
                .willReturn(userDetails);
        given(jwtTokenProvider.generateAccessToken(userDetails))
                .willReturn("access.jwt.token");
        given(jwtTokenProvider.generateRefreshToken(userDetails, true))
                .willReturn("refresh.jwt.token");

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@admin.com")
                        .param("password", "Admin1234!")
                        .param("rememberMe", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.userDto.email").value("admin@admin.com"));

        verify(jwtTokenProvider).generateRefreshToken(userDetails, true);
        verify(jwtTokenProvider).addRefreshCookie(
                any(HttpServletResponse.class),
                eq("refresh.jwt.token"),
                eq(true)
        );
        verify(authenticationAttemptService).resetLoginFailures("admin@admin.com");
    }

    @Test
    @DisplayName("/api/auth/sign-in 로그인 실패 - 정지된 사용자")
    void signInFailure_BannedUser() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-05-08T00:00:00Z"),
                "user@mopl.com",
                "정지사용자",
                null,
                Role.USER,
                false,
                true
        );

        MoplUserDetails userDetails = new MoplUserDetails(
                userDto,
                passwordEncoder.encode("User1234!")
        );

        given(userDetailsService.loadUserByUsername("user@mopl.com"))
                .willReturn(userDetails);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "user@mopl.com")
                        .param("password", "User1234!"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));

        verify(jwtTokenProvider, never()).generateAccessToken(any());
        verify(jwtRegistry, never()).registerJwtInformation(any());
    }

    @Test
    @DisplayName("/api/auth/sign-in 로그인 실패 - 잘못된 비밀번호")
    void signInFailure_InvalidPassword() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-05-08T00:00:00Z"),
                "admin@admin.com",
                "관리자",
                null,
                Role.ADMIN,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(
                userDto,
                passwordEncoder.encode("Admin1234!")
        );

        given(userDetailsService.loadUserByUsername("admin@admin.com"))
                .willReturn(userDetails);
        given(authenticationAttemptService.recordLoginFailure("admin@admin.com"))
                .willReturn(false);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@admin.com")
                        .param("password", "WrongPassword1!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("ID/PW가 올바르지 않습니다."));

        verify(authenticationAttemptService).recordLoginFailure("admin@admin.com");
    }

    @Test
    @DisplayName("/api/auth/sign-in 로그인 실패 - 실패 횟수 초과로 로그인 제한")
    void signInFailure_LoginAttemptLocked() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                Instant.parse("2026-05-08T00:00:00Z"),
                "admin@admin.com",
                "관리자",
                null,
                Role.ADMIN,
                false,
                false
        );

        MoplUserDetails userDetails = new MoplUserDetails(
                userDto,
                passwordEncoder.encode("Admin1234!")
        );

        given(userDetailsService.loadUserByUsername("admin@admin.com"))
                .willReturn(userDetails);
        given(authenticationAttemptService.recordLoginFailure("admin@admin.com"))
                .willReturn(true);
        given(userRepository.findByEmail("admin@admin.com"))
                .willReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@admin.com")
                        .param("password", "WrongPassword1!"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("LOGIN_LOCKED"))
                .andExpect(jsonPath("$.message").value("비밀번호를 5회 틀려 30분간 로그인이 제한됩니다."));

        verify(authenticationAttemptService).recordLoginFailure("admin@admin.com");
        verify(userRepository).findByEmail("admin@admin.com");
    }

    @Test
    @DisplayName("/api/auth/sign-in 로그인 실패 - 존재하지 않는 사용자")
    void signInFailure_UserNotFound() throws Exception {
        given(userDetailsService.loadUserByUsername("missing@mopl.com"))
                .willThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다."
                ));
        given(authenticationAttemptService.recordLoginFailure("missing@mopl.com"))
                .willReturn(false);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "missing@mopl.com")
                        .param("password", "Admin1234!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));

        verify(authenticationAttemptService).recordLoginFailure("missing@mopl.com");
    }

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        DaoAuthenticationProvider authenticationProvider(
                MoplUserDetailsService userDetailsService,
                PasswordEncoder passwordEncoder
        ) {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
            provider.setUserDetailsService(userDetailsService);
            provider.setPasswordEncoder(passwordEncoder);
            return provider;
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                DaoAuthenticationProvider authenticationProvider,
                JwtLoginSuccessHandler jwtLoginSuccessHandler,
                LoginFailureHandler loginFailureHandler
        ) throws Exception {
            http
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/sign-in"))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .formLogin(login -> login
                            .loginProcessingUrl("/api/auth/sign-in")
                            .successHandler(jwtLoginSuccessHandler)
                            .failureHandler(loginFailureHandler)
                            .permitAll()
                    )
                    .authenticationProvider(authenticationProvider);

            return http.build();
        }
    }
}