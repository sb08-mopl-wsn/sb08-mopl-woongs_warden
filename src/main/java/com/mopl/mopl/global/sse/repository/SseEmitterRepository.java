package com.mopl.mopl.global.sse.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Repository
public class SseEmitterRepository {

  // 한 유저가 동시에 연결할 수 있는 최대 기기(브라우저 탭 등) 수 제한
  private static final int MAX_EMITTERS_PER_USER = 5;

  // key : 사용자 UUID , Value : SseEmitter 객체리스트 (다중 연결 지원)
  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  /* 특정 사용자의 Emitter를 리스트 저장
  * 메모리 누수 방지로 최대 연결 개수 제한*/
  public SseEmitter save(UUID userId, SseEmitter emitter) {

    List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

    // FIFO로 최대 개수 도달 시, 가장 오래된 연결 강제로 끊고 리스트에서 제거
    if (userEmitters.size() >= MAX_EMITTERS_PER_USER) {
      SseEmitter oldestEmitter = userEmitters.remove(0); // 맨 앞의 오래된 연결
      try {
        oldestEmitter.complete(); // 클라이언트에 연결 종료 알림
        log.info("SSE 최대 연결 수 초과로 가장 오래된 연결 강제 종료 - userId: {}", userId);
      } catch (Exception e) {
        log.warn("오래된 SSE 연결 강제 종료 중 에러 발생 - userId: {}", userId);
      }
    }

    userEmitters.add(emitter);
    return emitter;
  }

  // 특정 사용자의 모든 연결된 Emitter 리스트 조회
  public List<SseEmitter> findAllByUserId(UUID userId) {

    return emitters.getOrDefault(userId, Collections.emptyList());
  }

  // 특정 사용자의 특정 Emitter 하나 삭제 (연결 종료, 타임아웃)
  public void delete(UUID userId, SseEmitter emitter) {

    List<SseEmitter> userEmitters = emitters.get(userId);
    if (userEmitters != null) {
      userEmitters.remove(emitter);
      // 빈 리스트는 맵에서 키 지워서 메모리 누수 방지
      if (userEmitters.isEmpty()) {
        emitters.remove(userId);
      }
    }
  }

  // keep-Alive를 위한 현재 접속중인 모든 파이프 맵 반환
  public Map<UUID, List<SseEmitter>> findAllEmitters() {
    return emitters;
  }
}
