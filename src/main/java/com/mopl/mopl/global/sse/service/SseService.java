package com.mopl.mopl.global.sse.service;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.SseConnectionException;
import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

    // null 검증 추가
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");

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

    // 503 에러 방지 더미 이벤트(Init) 전송. 명시적으로 예외를 던져서 좀비 커넥션 방지
    try {

      emitter.send(SseEmitter.event()
          .name("connect")
          .data("연결 성공. Event Stream Created"));
    } catch (IOException e) {

      log.warn("SSE 초기 이벤트 전송 실패 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
      throw new SseConnectionException(e);
    }

    return emitter;
  }

  /**
   * 특정 사용자(모든 기기)에게 알림 객체를 전송
   * 이후 알림 도메인에서 호출.
   * @param userId 알림을 받을 사용자 ID
   * @param notificationData 알림 데이터 객체
   */
  public void sendNotification(UUID userId, Object notificationData) {

    sendCustomNotification(userId, "notifications", notificationData);
  }

  /**
   * 외부에서 호출 가능한 범용 알림 메서드
   * @param userId 알림을 받을 사용자 ID
   * @param eventName 이벤트 이름
   * @param data 전송할 데이터
   */
  public void sendCustomNotification(UUID userId, String eventName, Object data) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(eventName, "eventName은 null일 수 없습니다.");
    if (eventName.isBlank()) {
      throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "eventName은 빈 문자열일 수 없습니다.");
    }
    Objects.requireNonNull(data, "data는 null일 수 없습니다.");

    List<SseEmitter> emitters = emitterRepository.findAllByUserId(userId);
    if (emitters.isEmpty()) return;

    // 성공 횟수 집계
    long successCount = emitters.stream()
            .filter(emitter -> sendToClient(emitter, userId, eventName, data))
            .count();

    // 다중 기기 중 일부라도 실패하거나 전체 성공 현황 info 기록
    log.info("SSE 알림 전송 완료 - userId: {}, eventName: {}, 성공: {}/{}", userId, eventName, successCount, emitters.size());

    // TODO: 분산 환경 전환 시 위 단일 서버 환경 로직 대신 Redis Pub/Sub 구조 도입 후 메시지 발행하도록 수정
  }

  // heartbeat 발송기 (1개 스레드로 30초마다 모든 유저에게 핑 발송)
  @Scheduled(fixedRate = 30000)
  public void sendHeartbeat() {
    Map<UUID, List<SseEmitter>> allEmitters = emitterRepository.findAllEmitters();
    if (allEmitters.isEmpty()) return;

    allEmitters.forEach((userId, emitterList) -> {
      emitterList.forEach(emitter -> {
        try {
          emitter.send(SseEmitter.event()
              .name("heartbeat")
              .data("ping"));
        } catch (Exception e) {
          emitterRepository.delete(userId, emitter);
        }
      });
    });
  }

  /**
   * 공통 발송 로직
   * @param emitter Emitter 객체
   * @param userId 연결을 요청한 유저 ID
   * @param eventName 이벤트 이름
   * @param data 전송할 데이터
   * @return 전송 성공 여부
   */
  private boolean sendToClient(SseEmitter emitter, UUID userId, String eventName, Object data) {
    try {
      // TODO: Redis 도입 시 프론트엔드 재연결(Last-Event-ID) 처리를 위한 순차적 ID(timestamp+sequence) 저장소와 함께 .id() 구현 예정
      emitter.send(SseEmitter.event()
          .name(eventName)
          .data(data));
      return true; // 전송 성공
    } catch (IOException e) {
      log.info("SSE 연결이 이미 끊어졌거나 전송 실패 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
      return false; // 전송 실패
    }
  }
}
