package com.mopl.mopl.global.sse.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

  // key : 사용자 UUID , Value : SseEmitter 객체리스트 (다중 연결 지원)
  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  // 특정 사용자의 Emitter를 리스트 저장
  public SseEmitter save(UUID userId, SseEmitter emitter) {

    emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    return emitter;
  }

  // 특정 사용자의 모든 연결된 Emitter 리스트 조회
  public List<SseEmitter> findAllByUserId(UUID userId) {

    return emitters.getOrDefault(userId, new CopyOnWriteArrayList<>());
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
}
