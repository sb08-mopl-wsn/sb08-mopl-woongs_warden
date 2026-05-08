package com.mopl.mopl.domain.jwt.registry;

import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
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
    private final Map<String, JwtInformation> tokenIndex;

    public InMemoryJwtRegistry(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.origin = new ConcurrentHashMap<>();
        this.maxActiveJwtCount = 1;
        this.tokenIndex =  new ConcurrentHashMap<>();
    }

    @Override
    @Transactional
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUser().id();
        origin.putIfAbsent(userId, new ConcurrentLinkedQueue<>());
        Queue<JwtInformation> userQueue = origin.get(userId);

        while (userQueue.size() >= maxActiveJwtCount) {
            JwtInformation removed = userQueue.poll();
            if (removed != null) {
                tokenIndex.remove(removed.getAccessToken());
            }
        }

        // 3. 새 로그인 정보 메모리(큐)에 추가
        userQueue.offer(jwtInformation);
        tokenIndex.put(jwtInformation.getAccessToken(), jwtInformation);
    }

    @Override
    @Transactional
    // 유저 큐삭제
    public void invalidateJwtInformationByUserId(UUID userId) {
        Queue<JwtInformation> removedQueue = origin.remove(userId);

        if (removedQueue != null) {
            removedQueue.forEach(info -> tokenIndex.remove(info.getAccessToken()));
        }
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        Queue<JwtInformation> userQueue = origin.get(userId);
        return userQueue != null && !userQueue.isEmpty();
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) return false;
        return tokenIndex.containsKey(accessToken);
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

        origin.putIfAbsent(userId, new ConcurrentLinkedQueue<>());
        Queue<JwtInformation> userQueue = origin.get(userId);

        JwtInformation[] removedHolder = new JwtInformation[1];

        userQueue.removeIf(info -> {
            boolean matched = refreshToken.equals(info.getRefreshToken());

            if (matched) {
                removedHolder[0] = info;
            }

            return matched;
        });

        JwtInformation removed = removedHolder[0];

        if (removed != null) {
            tokenIndex.remove(removed.getAccessToken());
        }

        while (userQueue.size() >= maxActiveJwtCount) {
            JwtInformation polled = userQueue.poll();

            if (polled != null) {
                tokenIndex.remove(polled.getAccessToken());
            }
        }

        userQueue.offer(newJwtInformation);
        tokenIndex.put(newJwtInformation.getAccessToken(), newJwtInformation);
    }

    @Override
    @Scheduled(fixedDelay = 1000 * 60 * 5)
    public void clearExpiredJwtInformation() {
        Date now = new Date();

        origin.values().forEach(queue -> {
            queue.removeIf(info -> {
                boolean shouldRemove;

                try {
                    boolean isAccessExpired = jwtTokenProvider.getExpiration(info.getAccessToken()).before(now);
                    boolean isRefreshExpired = jwtTokenProvider.getExpiration(info.getRefreshToken()).before(now);

                    shouldRemove = isAccessExpired && isRefreshExpired;
                } catch (Exception e) {
                    shouldRemove = true;
                }

                if (shouldRemove) {
                    tokenIndex.remove(info.getAccessToken());
                }

                return shouldRemove;
            });
        });
    }
}