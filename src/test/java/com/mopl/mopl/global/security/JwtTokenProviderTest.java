package com.mopl.mopl.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "access-secret-key-must-be-at-least-32-bytes-1234567890",
            900_000,
            "refresh-secret-key-must-be-at-least-32-bytes-1234567890",
            3_600_000,
            false,
            "Lax"
    );

    JwtTokenProviderTest() throws com.nimbusds.jose.JOSEException {
    }

    @Test
    @DisplayName("Access Token 발급 시 subject와 userEmail claim에 email이 들어간다")
    void generateAccessToken_ContainsEmailClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);

        String accessToken = tokenProvider.generateAccessToken(userDetails);

        assertThat(tokenProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(accessToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserEmailFromToken(accessToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserIdFromToken(accessToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Refresh Token 발급 시 type=refresh 토큰으로 검증된다")
    void generateRefreshToken_ValidatesAsRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);

        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        assertThat(tokenProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(tokenProvider.validateAccessToken(refreshToken)).isFalse();
        assertThat(tokenProvider.getUserEmailFromToken(refreshToken)).isEqualTo("admin@admin.com");
        assertThat(tokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("parseAccessToken은 email과 name을 분리해서 MoplUserDetails를 만든다")
    void parseAccessToken_SeparatesEmailAndName() throws Exception {
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = userDetails(userId, "admin@admin.com", "관리자", Role.ADMIN);
        String accessToken = tokenProvider.generateAccessToken(userDetails);

        MoplUserDetails parsed = tokenProvider.parseAccessToken(accessToken);

        assertThat(parsed.getUserDto().id()).isEqualTo(userId);
        assertThat(parsed.getUserDto().email()).isEqualTo("admin@admin.com");
        assertThat(parsed.getUserDto().name()).isEqualTo("관리자");
        assertThat(parsed.getUserDto().role()).isEqualTo(Role.ADMIN);
        assertThat(parsed.getUsername()).isEqualTo("admin@admin.com");
    }

    private MoplUserDetails userDetails(UUID userId, String email, String name, Role role) {
        UserDto userDto = new UserDto(
                userId,
                Instant.parse("2026-05-08T00:00:00Z"),
                email,
                name,
                null,
                role,
                false
        );
        return new MoplUserDetails(userDto, "encoded-password");
    }
}
