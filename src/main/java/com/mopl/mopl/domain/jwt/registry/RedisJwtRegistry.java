package com.mopl.mopl.domain.jwt.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.jwt.registry", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisJwtRegistry implements JwtRegistry {
    private static final String USER_REFRESH_SET_PREFIX = "jwt:user:";
    private static final String REFRESH_INFO_PREFIX = "jwt:refresh:";
    private static final String ACCESS_ACTIVE_PREFIX = "jwt:access:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUser().id();
        String userKey = userSetKey(userId);

        Set<String> existingRefreshTokens = redisTemplate.opsForSet().members(userKey);
        if (existingRefreshTokens != null) {
            existingRefreshTokens.forEach(this::deleteByRefreshToken);
        }

        saveJwtInformation(jwtInformation);
    }

    @Override
    @Transactional
    public void invalidateJwtInformationByUserId(UUID userId) {
        String userKey = userSetKey(userId);
        Set<String> refreshTokens = redisTemplate.opsForSet().members(userKey);
        if (refreshTokens != null) {
            refreshTokens.forEach(this::deleteByRefreshToken);
        }
        redisTemplate.delete(userKey);
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) {
            return false;
        }
        Boolean hasKey = redisTemplate.hasKey(accessActiveKey(accessToken));
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return false;
        }
        Boolean hasKey = redisTemplate.hasKey(refreshInfoKey(refreshToken));
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public JwtInformation getJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return null;
        }

        String serialized = redisTemplate.opsForValue().get(refreshInfoKey(refreshToken));
        if (serialized == null) {
            return null;
        }

        try {
            return objectMapper.readValue(serialized, JwtInformation.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JwtInformation from Redis. refreshToken={}", refreshToken, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        deleteByRefreshToken(refreshToken);
        saveJwtInformation(newJwtInformation);
    }

    @Override
    @Transactional
    public void rollbackRotateJwtInformation(
            String oldRefreshToken,
            JwtInformation oldJwtInformation,
            String newRefreshToken
    ) {
        if (newRefreshToken != null) {
            deleteByRefreshToken(newRefreshToken);
        }

        if (oldJwtInformation == null || oldRefreshToken == null) {
            return;
        }

        if (!hasActiveJwtInformationByRefreshToken(oldRefreshToken)) {
            saveJwtInformation(oldJwtInformation);
        }
    }

    private void saveJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUser().id();
        String refreshToken = jwtInformation.getRefreshToken();
        String accessToken = jwtInformation.getAccessToken();

        Date refreshExp = jwtTokenProvider.getExpiration(refreshToken);
        Date accessExp = jwtTokenProvider.getExpiration(accessToken);

        Duration refreshTtl = ttlUntil(refreshExp);
        Duration accessTtl = ttlUntil(accessExp);

        try {
            String serialized = objectMapper.writeValueAsString(jwtInformation);
            redisTemplate.opsForValue().set(refreshInfoKey(refreshToken), serialized, refreshTtl);
            redisTemplate.opsForValue().set(accessActiveKey(accessToken), "1", accessTtl);
            redisTemplate.opsForSet().add(userSetKey(userId), refreshToken);
            redisTemplate.expire(userSetKey(userId), refreshTtl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JwtInformation", e);
        }
    }

    private void deleteByRefreshToken(String refreshToken) {
        JwtInformation stored = getJwtInformationByRefreshToken(refreshToken);
        redisTemplate.delete(refreshInfoKey(refreshToken));

        if (stored != null) {
            redisTemplate.delete(accessActiveKey(stored.getAccessToken()));
            redisTemplate.opsForSet().remove(userSetKey(stored.getUser().id()), refreshToken);
        }
    }

    private Duration ttlUntil(Date expiration) {
        long millis = expiration.getTime() - System.currentTimeMillis();
        return Duration.ofMillis(Math.max(millis, 1L));
    }

    private String userSetKey(UUID userId) {
        return USER_REFRESH_SET_PREFIX + userId;
    }

    private String refreshInfoKey(String refreshToken) {
        return REFRESH_INFO_PREFIX + refreshToken;
    }

    private String accessActiveKey(String accessToken) {
        return ACCESS_ACTIVE_PREFIX + accessToken;
    }
}
