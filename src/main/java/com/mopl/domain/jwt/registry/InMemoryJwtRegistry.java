package com.mopl.domain.jwt.registry;

import com.mopl.domain.jwt.JwtTokenProvider;
import com.mopl.domain.jwt.dto.JwtInformation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class InMemoryJwtRegistry implements JwtRegistry {
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<UUID, Queue<JwtInformation>> origin;
    private final int maxActiveJwtCount;

    public InMemoryJwtRegistry(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.origin = new ConcurrentHashMap<>();
        this.maxActiveJwtCount = 1;
    }

    @Override
    @Transactional
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUser().id();
        origin.putIfAbsent(userId, new ConcurrentLinkedQueue<>());
        Queue<JwtInformation> userQueue = origin.get(userId);

        while (userQueue.size() >= maxActiveJwtCount) {
            JwtInformation oldInfo = userQueue.poll();
        }

        // 3. 새 로그인 정보 메모리(큐)에 추가
        userQueue.offer(jwtInformation);
    }

    @Override
    @Transactional
    public void invalidateJwtInformationByUserId(UUID userId) {
        origin.remove(userId);// 메모리에서 유저의 큐 자체를 삭제
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        Queue<JwtInformation> userQueue = origin.get(userId);
        return userQueue != null && !userQueue.isEmpty();
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) return false;

        // 전체 큐를 순회하며 해당 Access Token이 존재하는지 확인
        return origin.values().stream()
                .flatMap(Queue::stream)
                .anyMatch(info -> accessToken.equals(info.getAccessToken()));
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return false;

        // 전체 큐를 순회하며 해당 Refresh Token이 존재하는지 확인
        return origin.values().stream()
                .flatMap(Queue::stream)
                .anyMatch(info -> refreshToken.equals(info.getRefreshToken()));
    }

    @Override
    @Transactional
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        UUID userId = newJwtInformation.getUser().id();
        Queue<JwtInformation> userQueue = origin.get(userId);

        if (userQueue != null) {
            // 1. 기존 Refresh Token을 가진 정보 삭제
            userQueue.removeIf(info -> refreshToken.equals(info.getRefreshToken()));

            // 2. 갱신된 새로운 토큰 정보 추가
            userQueue.offer(newJwtInformation);

            // 3. DB 로테이션 수행
            String oldJti = jwtTokenProvider.getTokenId(refreshToken);
            String newJti = jwtTokenProvider.getTokenId(newJwtInformation.getRefreshToken());
        }
    }

    @Override
    @Scheduled(fixedDelay = 1000 * 60 * 5)
    public void clearExpiredJwtInformation() {
        Date now = new Date();

        // 주기적으로(5분마다) Map을 순회하며 만료된 토큰을 메모리에서 제거
        origin.values().forEach(queue -> {
            queue.removeIf(info -> {
                try {
                    // Provider의 getExpiration 활용하여 현재 시간과 비교
                    boolean isAccessExpired = jwtTokenProvider.getExpiration(info.getAccessToken()).before(now);
                    boolean isRefreshExpired = jwtTokenProvider.getExpiration(info.getRefreshToken()).before(now);

                    // 두 토큰 모두 만료되었다면 큐에서 제거
                    return isAccessExpired && isRefreshExpired;
                } catch (Exception e) {
                    // 파싱 실패(손상된 토큰 등) 시 유효하지 않으므로 제거 대상으로 간주
                    return true;
                }
            });
        });
    }
}