package com.mopl.domain.jwt.registry;

import com.sprint.mission.mopl.security.jwt.JwtInformation;

import java.util.UUID;

public interface JwtRegistry {
    void registerJwtInformation(JwtInformation jwtInformation);

    void invalidateJwtInformationByUserId(UUID userId);

    boolean hasActiveJwtInformationByUserId(UUID userId);

    boolean hasActiveJwtInformationByAccessToken(String accessToken);

    boolean hasActiveJwtInformationByRefreshToken(String refreshToken);

    void rotateJwtInformation(String refreshToken,JwtInformation  newJwtInformation);

    void clearExpiredJwtInformation();
}
