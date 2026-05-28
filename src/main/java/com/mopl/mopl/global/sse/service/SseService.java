package com.mopl.mopl.global.sse.service;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.global.exception.SseConnectionException;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.redis.service.RedisPublisher;
import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

  private final SseEmitterRepository emitterRepository;
  private final RedisPublisher redisPublisher;

  // 만료 시간 30분 설정
  private static final Long TIMEOUT = 30L * 1000 * 60;
  private static final String SSE_HISTORY_PREFIX = "sse_history:";
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * SSE 연결 요청
   * @param userId 연결을 요청한 유저 ID
   * @return SSE 연결 객체
   */
  public SseEmitter subscribe(UUID userId, String lastEventId) {

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

      // Last-Event-ID가 존재한다면 (재접속), 유실된 알림 복구 진행
      if (lastEventId != null && !lastEventId.isEmpty()) {
        resendMissedEvents(userId, lastEventId, emitter);
      }
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

    long currentTime = System.currentTimeMillis();
    String eventId = String.valueOf(currentTime);

    RedisPubMessage message = new RedisPubMessage(userId, eventName, data);

    // 알림 유실 방지로 Redis ZSet에 알림 내역 임시 저장
    saveEventToHistory(userId, message, currentTime);

    // 브로드캐스팅 발송
    redisPublisher.publishSse(message);
  }

  public void sendToLocalClient(UUID userId, String eventName, Object data) {

    List<SseEmitter> emitters = emitterRepository.findAllByUserId(userId);
    if (emitters.isEmpty()) return;

    // 수신 시간 기준으로 eventId 생성 (프론트엔드가 보관할 ID)
    String eventId = String.valueOf(System.currentTimeMillis());

    long successCount = emitters.stream()
        .filter(emitter -> sendToClient(emitter, userId, eventId, eventName, data))
        .count();

    log.info("로컬 SSE 알림 전송 완료 - userId: {}, 성공: {}/{}", userId, successCount, emitters.size());
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
          log.debug("Heartbeat 전송 실패로 emitter 제거 - userId: {}", userId, e);
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
  private boolean sendToClient(SseEmitter emitter, UUID userId, String eventId, String eventName, Object data) {
    try {
      emitter.send(SseEmitter.event()
          .id(eventId)
          .name(eventName)
          .data(data));
      return true; // 전송 성공
    } catch (IOException e) {
      log.info("SSE 연결이 이미 끊어졌거나 전송 실패 - userId: {}", userId);
      emitterRepository.delete(userId, emitter);
      return false; // 전송 실패
    }
  }

  /**
   * 유실된 알림 복구용 Redis 내역 저장
   * @param userId 연결을 요청한 유저 ID
   * @param message 알림 데이터
   * @param score 알림 발송 시간
   */
  private void saveEventToHistory(UUID userId, RedisPubMessage message, long score) {

    String historyKey = SSE_HISTORY_PREFIX + userId.toString();
    try {
      // ZSet 저장 (데이터, score)
      redisTemplate.opsForZSet().add(historyKey, message, score);
      // 저장소 용량 관리 -> 만료 시간 10분 설정
      redisTemplate.expire(historyKey, 10, TimeUnit.MINUTES);
    } catch (Exception e) {
      log.error("알림 히스토리 Redis 저장 실패 - userId: {}", userId, e);
    }
  }

  private void resendMissedEvents(UUID userId, String lastEventId, SseEmitter emitter) {

    String historyKey = SSE_HISTORY_PREFIX + userId.toString();
    try {
      // 프론트엔드가 제시한 마지막 시간을 넘어간 알림들만 추출
      long lastEventScore = 0;
      try {
        lastEventScore = Long.parseLong(lastEventId);
      } catch (NumberFormatException e) {
        log.warn("잘못된 Last-Event-ID 형식 - userId: {}, lastEventId: {}", userId, lastEventId);
      }

      // score(시간) 범위 검색: lastEventScore + 1 부터 끝까지
      Set<Object> missedEvents = redisTemplate.opsForZSet().rangeByScore(historyKey, lastEventScore + 1, Double.MAX_VALUE);

      if (missedEvents != null && !missedEvents.isEmpty()) {
        log.info("유실된 SSE 알림 복구 시작 - userId: {}, 유실 건수: {}", userId, missedEvents.size());

        for (Object obj : missedEvents) {
          if (obj instanceof RedisPubMessage pubMessage) {
            // 저장된 객체를 그대로 파싱해서 밀어넣음. 재전송 시 현재시간으로 새로운 id부여 (유실 대비)
            sendToClient(emitter, userId, String.valueOf(System.currentTimeMillis()), pubMessage.eventName(), pubMessage.data());
          } else {
            log.warn("예상치 못한 히스토리 객체 타입 - userId: {}, type: {}", userId, obj.getClass().getName());
          }
        }
      }
    } catch (Exception e) {
      log.warn("유실된 SSE 알림 복구 실패 - userId: {}", userId, e);
    }
  }
}
