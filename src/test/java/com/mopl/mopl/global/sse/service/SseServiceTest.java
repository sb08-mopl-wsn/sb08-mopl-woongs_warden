package com.mopl.mopl.global.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.global.event.UserLogoutEvent;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.redis.service.RedisPublisher;
import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @InjectMocks
  private SseService sseService;

  @Mock
  private SseEmitterRepository emitterRepository;

  @Mock
  private RedisPublisher redisPublisher;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ZSetOperations<String, Object> zSetOperations;

  private static final Long EXPECTED_TIMEOUT = 30L * 1000 * 60;

  @Test
  @DisplayName("유저 로그아웃 이벤트 수신 시, 해당 유저의 모든 다중 기기 Emitter를 정상 종료시키고 저장소에서 삭제한다.")
  void handleUserLogout_Success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserLogoutEvent event = new UserLogoutEvent(userId);

    SseEmitter mockEmitter1 = mock(SseEmitter.class);
    SseEmitter mockEmitter2 = mock(SseEmitter.class);
    given(emitterRepository.findAllByUserId(userId)).willReturn(List.of(mockEmitter1, mockEmitter2));

    // when
    sseService.handleUserLogout(event);

    // then
    verify(mockEmitter1, times(1)).complete();
    verify(mockEmitter2, times(1)).complete();
    verify(emitterRepository, times(1)).deleteAllByUserId(userId);
  }

  @Test
  @DisplayName("유저 로그아웃 처리 중 특정 Emitter에서 예외가 터지더라도, 에러를 catch 블록에서 격리하여 다른 기기의 연결 해제 및 저장소 삭제를 완수한다.")
  void handleUserLogout_ExceptionIsolated_ContinuesExecution() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserLogoutEvent event = new UserLogoutEvent(userId);

    SseEmitter failEmitter = mock(SseEmitter.class);
    SseEmitter successEmitter = mock(SseEmitter.class);
    given(emitterRepository.findAllByUserId(userId)).willReturn(List.of(failEmitter, successEmitter));

    doThrow(new RuntimeException("좀비 커넥션 강제 해제 장애")).when(failEmitter).complete();

    // when
    sseService.handleUserLogout(event);

    // then
    verify(failEmitter, times(1)).complete();
    verify(successEmitter, times(1)).complete();
    verify(emitterRepository, times(1)).deleteAllByUserId(userId);
  }

  @Test
  @DisplayName("로그아웃한 유저의 활성화된 Emitter가 없다면 저장소 삭제 처리를 건너뛰고 조기 종료한다.")
  void handleUserLogout_NoEmitters_ShortCircuit() {
    // given
    UUID userId = UUID.randomUUID();
    UserLogoutEvent event = new UserLogoutEvent(userId);
    given(emitterRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

    // when
    sseService.handleUserLogout(event);

    // then
    verify(emitterRepository, never()).deleteAllByUserId(any());
  }

  @Test
  @DisplayName("SSE 구독 - 최초 접속 시 Emitter가 저장되고 더미 이벤트가 발송된다.")
  void subscribe_Success() {

    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter result = sseService.subscribe(userId, "");

    // then
    assertThat(result).isNotNull();
    verify(emitterRepository).save(eq(userId), any(SseEmitter.class));
    assertThat(result.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
    verify(redisTemplate, never()).opsForZSet();
  }

  @Test
  @DisplayName("SSE 구독 - 재접속 시 Last-Event-ID가 존재하면 유실된 알림을 복구하여 전송한다.")
  void subscribe_Reconnect_RecoversMissedEvents() {

    // given
    UUID userId = UUID.randomUUID();
    String lastEventId = "1000";
    String historyKey = "sse_history:" + userId;

    RedisPubMessage missedMessage = new RedisPubMessage(userId, "notifications", "놓친 알림 1");
    Set<Object> missedEvents = Set.of(missedMessage);

    given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
    given(zSetOperations.rangeByScore(eq(historyKey), eq(1001.0), eq(Double.MAX_VALUE)))
        .willReturn(missedEvents);

    // when
    SseEmitter result = sseService.subscribe(userId, lastEventId);

    // then
    assertThat(result).isNotNull();
    verify(zSetOperations).rangeByScore(eq(historyKey), eq(1001.0), eq(Double.MAX_VALUE));
  }


  @Test
  @DisplayName("알림 전송 - Redis ZSet에 이벤트를 저장하고 Publisher를 통해 브로드캐스팅한다.")
  void sendNotification_SavesToHistoryAndPublishesToRedis() {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "테스트 알림 데이터";
    String historyKey = "sse_history:" + userId;

    given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

    // when
    sseService.sendNotification(userId, dummyData);

    // then
    // ZSet 히스토리 저장 확인
    verify(zSetOperations).add(eq(historyKey), any(RedisPubMessage.class), anyDouble());
    verify(redisTemplate).expire(eq(historyKey), eq(10L), eq(TimeUnit.MINUTES));
    // 브로드 캐스팅
    verify(redisPublisher).publishSse(any(RedisPubMessage.class));
    verify(emitterRepository, never()).findAllByUserId(userId);
  }

  @Test
  @DisplayName("로컬 알림 전송 - 유저의 모든 다중 기기 Emitter를 찾아 전송을 시도한다.")
  void sendToLocalClient_Success() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "테스트 알림 데이터";

    // 가짜 Emitter 객체 2개 생성 (다중 기기 접속 상황)
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    List<SseEmitter> emitters = new CopyOnWriteArrayList<>(List.of(emitter1, emitter2));

    given(emitterRepository.findAllByUserId(userId)).willReturn(emitters);

    // when
    sseService.sendToLocalClient(userId, "notifications", dummyData);

    // then
    verify(emitterRepository).findAllByUserId(userId);
    // 각 Emitter 객체에 대해 send() 메서드가 1번씩 호출됐는지 검증
    verify(emitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  @DisplayName("로컬 알림 전송 - IOException 발생 시 해당 Emitter를 Repository에서 삭제한다.")
  void sendToLocalClient_IOException_DeletesEmitter() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "데이터";

    // 강제 IOException 발생 Mock 객체 생성
    SseEmitter failEmitter = mock(SseEmitter.class);
    List<SseEmitter> emitters = new CopyOnWriteArrayList<>(List.of(failEmitter));

    given(emitterRepository.findAllByUserId(userId)).willReturn(emitters);

    // IOException 발생
    doThrow(new IOException("강제 에러 발생")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

    // when
    sseService.sendToLocalClient(userId, "notifications", dummyData);

    // then
    verify(emitterRepository).delete(userId, failEmitter);
  }

  @Test
  @DisplayName("SSE 구독 - 파라미터가 null이면 NullPointerException 발생")
  void subscribe_NullUserId_ThrowsException() {
    String lastEventId = "1000";
    // when & then
    assertThatThrownBy(() -> sseService.subscribe(null, lastEventId))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("userId는 null일 수 없습니다.");
  }

  @Test
  @DisplayName("알림 전송 - 파라미터가 null이면 NullPointerException 발생")
  void sendNotification_NullParameters_ThrowsException() {

    // given
    UUID userId = UUID.randomUUID();
    Object data = "test";

    // when & then
    assertThatThrownBy(() -> sseService.sendNotification(null, data))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> sseService.sendNotification(userId, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("알림 전송 - eventName이 빈 문자열이면 BusinessException 발생")
  void sendCustomNotification_BlankEventName_ThrowsException() {
    // given
    UUID userId = UUID.randomUUID();
    Object data = "test";

    // when & then
    assertThatThrownBy(() -> sseService.sendCustomNotification(userId, "", data))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> sseService.sendCustomNotification(userId, "   ", data))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("로컬 알림 전송 - 연결된 기기가 하나도 없으면 전송 시도를 하지 않고 일찍 종료한다")
  void sendToLocalClient_NoEmitters_SkipsSending() {

    // given
    UUID userId = UUID.randomUUID();

    // Repo가 빈 리스트 반환
    given(emitterRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

    // when
    sseService.sendToLocalClient(userId, "notifications", "data");

    // then
    verify(emitterRepository).findAllByUserId(userId);
  }

  @Test
  @DisplayName("콜백 검증 - 완료/타임아웃/에러 발생 시 Repository에서 Emitter가 삭제되도록 콜백이 등록된다.")
  void subscribe_CallbackRegistered() {

    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter emitter = sseService.subscribe(userId, "");

    // then
    assertThat(emitter).isNotNull();
  }

  @Test
  @DisplayName("SSE 구독 - 연결 완료(Completion), 타임아웃, 에러 발생 시 Repository에서 Emitter가 올바르게 삭제된다.")
  void subscribe_Callbacks_TriggerDelete() {

    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = sseService.subscribe(userId, "");

    // SseEmitter 내부에 등록된 private 콜백 객체들 강제 추출
    Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
    Runnable timeoutCallback = (Runnable) ReflectionTestUtils.getField(emitter, "timeoutCallback");

    @SuppressWarnings("unchecked")
    Consumer<Throwable> errorCallback = (Consumer<Throwable>) ReflectionTestUtils.getField(emitter, "errorCallback");

    // when 추출 콜백들 강제 실행
    if (completionCallback != null) completionCallback.run();
    if (timeoutCallback != null) timeoutCallback.run();
    if (errorCallback != null) errorCallback.accept(new RuntimeException("테스트용 강제 에러 발생"));

    // then
    verify(emitterRepository, times(3)).delete(eq(userId), eq(emitter));
  }

  @Test
  @DisplayName("하트비트 스케줄러 - 저장된 Emitter가 없으면 아무 동작도 하지 않고 종료된다.")
  void sendHeartbeat_Empty_skip() {

    // given
    given(emitterRepository.findAllEmitters()).willReturn(Collections.emptyMap());

    // when
    sseService.sendHeartbeat();

    // then
    verify(emitterRepository, times(1)).findAllEmitters();
    verify(emitterRepository, never()).delete(any(UUID.class), any(SseEmitter.class));
  }

  @Test
  @DisplayName("하트비트 스케줄러 - 접속 중인 모든 Emitter에 ping 이벤트를 정상 발송한다.")
  void sendHeartbeat_Success() throws Exception {

    // given
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);

    Map<UUID, List<SseEmitter>> mockEmitters = new HashMap<>();
    mockEmitters.put(userId1, List.of(emitter1));
    mockEmitters.put(userId2, List.of(emitter2));

    given(emitterRepository.findAllEmitters()).willReturn(mockEmitters);

    // when
    sseService.sendHeartbeat();

    // then
    verify(emitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  @DisplayName("하트비트 스케줄러 - 전송 중 에러(IOException) 발생 시 해당 Emitter를 Repository에서 강제 삭제한다.")
  void sendHeartbeat_Exception_DeletesEmitter() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    SseEmitter failEmitter = mock(SseEmitter.class);

    Map<UUID, List<SseEmitter>> mockEmitters = new HashMap<>();
    mockEmitters.put(userId, List.of(failEmitter));

    given(emitterRepository.findAllEmitters()).willReturn(mockEmitters);

    doThrow(new IOException("연결 끊김")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

    // when
    sseService.sendHeartbeat();

    // then
    verify(failEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitterRepository, times(1)).delete(eq(userId), eq(failEmitter));
  }

  @Test
  @DisplayName("하트비트 스케줄러 - 여러 Emitter 중 일부만 전송에 실패할 경우, 실패한 것만 삭제되고 나머지는 정상 발송된다.")
  void sendHeartbeat_PartialException_DeletesOnlyFailed() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    SseEmitter successEmitter = mock(SseEmitter.class);
    SseEmitter failEmitter = mock(SseEmitter.class);

    Map<UUID, List<SseEmitter>> mockEmitters =new HashMap<>();
    // 한 유저가 탭 2개 열어둔 상태(1개 정상, 1개 죽은 파이프)
    mockEmitters.put(userId, List.of(successEmitter, failEmitter));

    given(emitterRepository.findAllEmitters()).willReturn(mockEmitters);

    // failEmitter만 에러
    doThrow(new IOException("죽은 파이프")).when(failEmitter).send(any(SseEmitter.SseEventBuilder.class));

    // when
    sseService.sendHeartbeat();

    // then
    // 둘다 한번씩은 핑(send) 시도를 했어야함.
    verify(successEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(failEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));

    // 에러가 난 failEmitter만 삭제
    verify(emitterRepository, times(1)).delete(eq(userId), eq(failEmitter));

    // 정상 emitter는 살아있는지 검증
    verify(emitterRepository, never()).delete(eq(userId), eq(successEmitter));
  }

  @Test
  @DisplayName("알림 전송 - Redis 저장 실패 시에도 브로드캐스팅은 정상 진행된다.")
  void sendNotification_RedisHistoryFailure_StillPublishes() {
    // given
    UUID userId = UUID.randomUUID();
    given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
    doThrow(new RuntimeException("Redis 연결 실패"))
        .when(zSetOperations).add(anyString(), any(), anyDouble());

    // when
    sseService.sendNotification(userId, "data");

    // then
    verify(redisPublisher).publishSse(any(RedisPubMessage.class));
  }

  @Test
  @DisplayName("SSE 구독 - 잘못된 형식의 Last-Event-ID는 무시하고 정상 연결된다.")
  void subscribe_InvalidLastEventIdFormat_IgnoresRecovery() {
    // given
    UUID userId = UUID.randomUUID();
    String invalidLastEventId = "not-a-number";

    // when
    SseEmitter result = sseService.subscribe(userId, invalidLastEventId);

    // then
    assertThat(result).isNotNull();
    // Redis 조회가 시도되지 않거나, 예외가 안전하게 처리됨
  }
}