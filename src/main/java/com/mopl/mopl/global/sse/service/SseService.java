package com.mopl.mopl.global.sse.service;

import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

  private final SseEmitterRepository emitterRepository;

  // 만료 시간 30분 설정
  private static final Long TIMEOUT = 30L * 1000 * 60;

  /**
   * SSE 연결 요청
   * @param userId 연결을 요청한 유저 ID
   * @return SSE 연결 객체
   */
  public SseEmitter subscribe(UUID userId) {

    // Emitter 객체 생성, 저장
    SseEmitter emitter = new SseEmitter(TIMEOUT);
    emitterRepository.save(userId, emitter);

    // 비동기 요청 완료, 타임아웃, 에러 시 Repo에서 안전하게 삭제되도록 콜백 등록
    emitter.onCompletion(() -> {
      log.debug("SSE 연결 완료 (정상 종료) - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
    });

    emitter.onTimeout(() -> {
      log.debug("SSE 연결 타임아웃 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
    });

    emitter.onError((e) -> {
      log.warn("SSE 연결 에러 발생 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
    });

    // 503 에러 방지 더미 이벤트(Init) 전송
    sendToClient(emitter, userId, "연결 성공. Event Stream Created");

    return emitter;
  }

  // 공통 발송 로직 : 단일 Emitter에 데이터를 전송

  /**
   * 공통 발송 로직
   * 단일 Emitter에 데이터 전송
   * @param emitter Emitter 객체
   * @param userId 연결을 요청한 유저 ID
   * @param data 전송할 데이터
   */
  private void sendToClient(SseEmitter emitter, UUID userId, Object data) {

    try {

      // notifications 이벤트로 데이터 전송
      emitter.send(SseEmitter.event()
          .name("notifications")
          .data(data));
    } catch (IOException e) {

      log.debug("SSE 연결이 이미 끊어졌거나 전송 실패 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
    }
  }

  /**
   * 특정 사용자(모든 기기)에게 알림 객체를 전송
   * 이후 알림 도메인에서 호출.
   * @param userId
   * @param notificationData
   */
  public void sendNotification(UUID userId, Object notificationData) {

    // 단일 서버 환경 -> 내 메모리에서 해당 유저의 모든 다중 기기 Emitter를 찾아서 전송
    List<SseEmitter> emitters = emitterRepository.findAllByUserId(userId);
    emitters.forEach(emitter -> sendToClient(emitter, userId, notificationData));

    // TODO: 분산 환경 전환 시 위 단일 서버 환경 로직 대신 Redis Pub/Sub 구조 도입 후 메시지 발행하도록 수정
  }
}
