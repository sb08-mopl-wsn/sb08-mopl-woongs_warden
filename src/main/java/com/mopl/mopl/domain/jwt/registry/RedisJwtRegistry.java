package com.mopl.mopl.domain.jwt.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.exception.JwtHashGenerationFailedException;
import com.mopl.mopl.domain.jwt.exception.JwtSerializationFailedException;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        Set<String> existingRefreshTokenHashes = redisTemplate.opsForSet().members(userKey);
        if (existingRefreshTokenHashes != null) {
            existingRefreshTokenHashes.forEach(this::deleteByRefreshTokenHash);
        }

        saveJwtInformation(jwtInformation);
    }

    @Override
    @Transactional
    public void invalidateJwtInformationByUserId(UUID userId) {
        String userKey = userSetKey(userId);
        Set<String> refreshTokenHashes = redisTemplate.opsForSet().members(userKey);
        if (refreshTokenHashes != null) {
            refreshTokenHashes.forEach(this::deleteByRefreshTokenHash);
        }
        redisTemplate.delete(userKey);
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) {
            return false;
        }

        Boolean hasHashedKey = redisTemplate.hasKey(accessActiveKey(tokenHash(accessToken)));
        if (Boolean.TRUE.equals(hasHashedKey)) {
            return true;
        }

        Boolean hasLegacyKey = redisTemplate.hasKey(accessActiveKey(accessToken));
        return Boolean.TRUE.equals(hasLegacyKey);
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return false;
        }
        Boolean hasHashedKey = redisTemplate.hasKey(refreshInfoKey(tokenHash(refreshToken)));
        if (Boolean.TRUE.equals(hasHashedKey)) {
            return true;
        }

        // Backward compatibility for legacy keys stored with raw refresh token
        Boolean hasLegacyKey = redisTemplate.hasKey(refreshInfoKey(refreshToken));
        return Boolean.TRUE.equals(hasLegacyKey);
    }

    @Override
    public JwtInformation getJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return null;
        }

        String serialized = redisTemplate.opsForValue().get(refreshInfoKey(tokenHash(refreshToken)));
        if (serialized == null) {
            // Backward compatibility for legacy keys stored with raw refresh token
            serialized = redisTemplate.opsForValue().get(refreshInfoKey(refreshToken));
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

    @Transactional
    public void clearAllJwtKeys() {
        Set<String> userKeys = redisTemplate.keys(USER_REFRESH_SET_PREFIX + "*");
        Set<String> refreshKeys = redisTemplate.keys(REFRESH_INFO_PREFIX + "*");
        Set<String> accessKeys = redisTemplate.keys(ACCESS_ACTIVE_PREFIX + "*");

        List<String> allKeys = new ArrayList<>();
        if (userKeys != null) {
            allKeys.addAll(userKeys);
        }
        if (refreshKeys != null) {
            allKeys.addAll(refreshKeys);
        }
        if (accessKeys != null) {
            allKeys.addAll(accessKeys);
        }

        if (!allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
        }
    }

    private void saveJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUser().id();
        String refreshToken = jwtInformation.getRefreshToken();
        String accessToken = jwtInformation.getAccessToken();

        String refreshTokenHash = tokenHash(refreshToken);
        String accessTokenHash = tokenHash(accessToken);

        Date refreshExp = jwtTokenProvider.getExpiration(refreshToken);
        Date accessExp = jwtTokenProvider.getExpiration(accessToken);

        Duration refreshTtl = ttlUntil(refreshExp);
        Duration accessTtl = ttlUntil(accessExp);

        try {
            String serialized = objectMapper.writeValueAsString(jwtInformation);
            redisTemplate.opsForValue().set(refreshInfoKey(refreshTokenHash), serialized, refreshTtl);
            redisTemplate.opsForValue().set(accessActiveKey(accessTokenHash), "1", accessTtl);
            redisTemplate.opsForSet().add(userSetKey(userId), refreshTokenHash);
            redisTemplate.expire(userSetKey(userId), refreshTtl);
        } catch (JsonProcessingException e) {
            throw new JwtSerializationFailedException();
        }
    }

    private void deleteByRefreshToken(String refreshToken) {
        String refreshTokenHash = tokenHash(refreshToken);
        JwtInformation stored = getJwtInformationByRefreshTokenHash(refreshTokenHash);

        if (stored != null) {
            deleteByRefreshTokenHash(refreshTokenHash);
            return;
        }

        // Backward compatibility for legacy keys stored with raw refresh token
        JwtInformation legacyStored = getJwtInformationByRefreshTokenRaw(refreshToken);
        redisTemplate.delete(refreshInfoKey(refreshToken));


        if (legacyStored != null) {
            redisTemplate.delete(accessActiveKey(legacyStored.getAccessToken()));
            redisTemplate.opsForSet().remove(userSetKey(legacyStored.getUser().id()), refreshToken);
        }
    }

    private JwtInformation getJwtInformationByRefreshTokenRaw(String refreshToken) {
        String serialized = redisTemplate.opsForValue().get(refreshInfoKey(refreshToken));
        if (serialized == null) {
            return null;
        }

        try {
            return objectMapper.readValue(serialized, JwtInformation.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JwtInformation from Redis legacy key. refreshToken={}", refreshToken, e);
            return null;
        }
    }

    private void deleteByRefreshTokenHash(String refreshTokenHash) {
        JwtInformation stored = getJwtInformationByRefreshTokenHash(refreshTokenHash);
        redisTemplate.delete(refreshInfoKey(refreshTokenHash));

        if (stored != null) {
            redisTemplate.delete(List.of(
                    accessActiveKey(stored.getAccessToken()),
                    accessActiveKey(tokenHash(stored.getAccessToken()))
            ));
            redisTemplate.opsForSet().remove(userSetKey(stored.getUser().id()), refreshTokenHash);
        }
    }

    private JwtInformation getJwtInformationByRefreshTokenHash(String refreshTokenHash) {
        String serialized = redisTemplate.opsForValue().get(refreshInfoKey(refreshTokenHash));
        if (serialized == null) {
            return null;
        }

        try {
            return objectMapper.readValue(serialized, JwtInformation.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JwtInformation from Redis. refreshTokenHash={}", refreshTokenHash, e);
            return null;
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

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new JwtHashGenerationFailedException();
        }
    }
}