package com.mopl.mopl.global.security;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "access-secret-key-must-be-at-least-32-bytes-1234567890",
            900_000,
            "refresh-secret-key-must-be-at-least-32-bytes-1234567890",
            3_600_000,
            604_800_000,
            false,
            "Lax"
    );

    JwtTokenProviderTest() throws com.nimbusds.jose.JOSEException {
    }

    @Test
    @DisplayName("Access Token 발급 시 subject와 userId claim이 들어간다")
    void generateAccessToken_containsClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);

        String accessToken = tokenProvider.generateAccessToken(userDetails);

        assertThat(tokenProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(tokenProvider.validateRefreshToken(accessToken)).isFalse();
        assertThat(tokenProvider.getUserEmailFromToken(accessToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserIdFromToken(accessToken)).isEqualTo(userId);
        assertThat(tokenProvider.getTokenId(accessToken)).isNotBlank();
        assertThat(tokenProvider.getExpiration(accessToken)).isNotNull();
    }

    @Test
    @DisplayName("Refresh Token 발급 시 type=refresh 토큰으로 검증된다")
    void generateRefreshToken_validatesAsRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);

        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        assertThat(tokenProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(tokenProvider.validateAccessToken(refreshToken)).isFalse();
        assertThat(tokenProvider.getUserEmailFromToken(refreshToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId);
        assertThat(tokenProvider.getTokenId(refreshToken)).isNotBlank();
        assertThat(tokenProvider.getExpiration(refreshToken)).isNotNull();
        assertThat(tokenProvider.isRememberMeRefreshToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("RememberMe Refresh Token 발급 시 rememberMe claim이 true로 들어간다")
    void generateRefreshToken_rememberMe_true() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);

        String refreshToken = tokenProvider.generateRefreshToken(userDetails, true);

        assertThat(tokenProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(tokenProvider.validateAccessToken(refreshToken)).isFalse();
        assertThat(tokenProvider.isRememberMeRefreshToken(refreshToken)).isTrue();
        assertThat(tokenProvider.getUserEmailFromToken(refreshToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("parseAccessToken은 JWT claim에서 사용자 정보를 복원한다")
    void parseAccessToken_restoresUserDetailsFromClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);
        String accessToken = tokenProvider.generateAccessToken(userDetails);

        MoplUserDetails parsed = tokenProvider.parseAccessToken(accessToken);

        assertThat(parsed.getUserDto().id()).isEqualTo(userId);
        assertThat(parsed.getUserDto().email()).isEqualTo("admin@admin.com");
        assertThat(parsed.getUserDto().name()).isEqualTo("관리자");
        assertThat(parsed.getUserDto().role()).isEqualTo(Role.ADMIN);
        assertThat(parsed.getUsername()).isEqualTo("admin@admin.com");
        assertThat(parsed.getPassword()).isNull();
    }

    @Test
    @DisplayName("Access Token은 Refresh Token 검증에 실패한다")
    void accessToken_failsRefreshValidation() throws Exception {
        MoplUserDetails userDetails = userDetails(
                UUID.randomUUID(),
                "user@test.com",
                "사용자",
                Role.USER
        );

        String accessToken = tokenProvider.generateAccessToken(userDetails);

        assertThat(tokenProvider.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token은 Access Token 검증에 실패한다")
    void refreshToken_failsAccessValidation() throws Exception {
        MoplUserDetails userDetails = userDetails(
                UUID.randomUUID(),
                "user@test.com",
                "사용자",
                Role.USER
        );

        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        assertThat(tokenProvider.validateAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰은 Access Token 검증에 실패한다")
    void invalidToken_failsAccessValidation() {
        assertThat(tokenProvider.validateAccessToken("invalid-token")).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰은 Refresh Token 검증에 실패한다")
    void invalidToken_failsRefreshValidation() {
        assertThat(tokenProvider.validateRefreshToken("invalid-token")).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰에서 rememberMe claim 조회 시 IllegalArgumentException이 발생한다")
    void invalidToken_isRememberMeRefreshToken_throwIllegalArgumentException() {
        assertThatThrownBy(() -> tokenProvider.isRememberMeRefreshToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Refresh Token 쿠키를 생성한다")
    void generateRefreshTokenCookie_success() {
        String refreshToken = "refresh.token.value";

        ResponseCookie cookie = tokenProvider.generateRefreshTokenCookie(refreshToken);

        assertThat(cookie.getName()).isEqualTo(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3_600);
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("RememberMe Refresh Token 쿠키를 생성하면 maxAge가 rememberMe 만료 시간으로 설정된다")
    void generateRefreshTokenCookie_rememberMe_success() {
        String refreshToken = "refresh.token.value";

        ResponseCookie cookie = tokenProvider.generateRefreshTokenCookie(refreshToken, true);

        assertThat(cookie.getName()).isEqualTo(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604_800);
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("Refresh Token 만료 쿠키를 생성한다")
    void generateRefreshTokenExpirationCookie_success() {
        ResponseCookie cookie = tokenProvider.generateRefreshTokenExpirationCookie();

        assertThat(cookie.getName()).isEqualTo(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.toString()).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("addRefreshCookie는 Set-Cookie 헤더를 추가한다")
    void addRefreshCookie_success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenProvider.addRefreshCookie(response, "refresh.token.value");

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(setCookie).contains("refresh.token.value");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("expireRefreshCookie는 maxAge 0인 Set-Cookie 헤더를 추가한다")
    void expireRefreshCookie_success() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        tokenProvider.expireRefreshCookie(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).contains(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    private MoplUserDetails userDetails(UUID userId, String email, String name, Role role) {
        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                email,
                name,
                null,
                role,
                false,
                false
        );

        return new MoplUserDetails(userDto, "encoded-password");
    }
}