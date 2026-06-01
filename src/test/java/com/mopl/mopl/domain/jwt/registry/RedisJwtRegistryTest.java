package com.mopl.mopl.domain.jwt.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisJwtRegistryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RedisJwtRegistry registry = new RedisJwtRegistry(
            redisTemplate,
            new ObjectMapper(),
            jwtTokenProvider
    );

    @Test
    @DisplayName("Access Token 활성 상태 확인 시 해시 키가 없으면 레거시 원문 키를 확인한다")
    void hasActiveJwtInformationByAccessToken_FallsBackToLegacyRawAccessTokenKey() {
        String accessToken = "legacy.access.token";
        String hashedAccessKey = "jwt:access:" + sha256Hex(accessToken);
        String legacyAccessKey = "jwt:access:" + accessToken;

        when(redisTemplate.hasKey(hashedAccessKey)).thenReturn(false);
        when(redisTemplate.hasKey(legacyAccessKey)).thenReturn(true);

        boolean result = registry.hasActiveJwtInformationByAccessToken(accessToken);

        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(hashedAccessKey);
        verify(redisTemplate).hasKey(legacyAccessKey);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
