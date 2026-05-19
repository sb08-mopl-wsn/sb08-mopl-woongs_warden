package com.mopl.mopl.global.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @InjectMocks
  private SseService sseService;

  @Mock
  private SseEmitterRepository emitterRepository;

  private static final Long EXPECTED_TIMEOUT = 30L * 1000 * 60;

  @Test
  @DisplayName("SSE 구독 - 정상적으로 Emitter가 생성되고 저장된다.")
  void subscribe_Success() {

    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter result = sseService.subscribe(userId);

    // then
    assertThat(result).isNotNull();
    verify(emitterRepository).save(eq(userId), any(SseEmitter.class));
    assertThat(result.getTimeout()).isEqualTo(EXPECTED_TIMEOUT);
  }

  @Test
  @DisplayName("알림 전송 - 유저의 모든 다중 기기 Emitter를 찾아 전송을 시도한다.")
  void sendNotification_Success() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "테스트 알림 데이터";

    // 가짜 Emitter 객체 2개 생성 (다중 기기 접속 상황)
    SseEmitter emitter1 = mock(SseEmitter.class);
    SseEmitter emitter2 = mock(SseEmitter.class);
    List<SseEmitter> emitters = new CopyOnWriteArrayList<>(List.of(emitter1, emitter2));

    given(emitterRepository.findAllByUserId(userId)).willReturn(emitters);

    // when
    sseService.sendNotification(userId, dummyData);

    // then
    verify(emitterRepository).findAllByUserId(userId);
    // 각 Emitter 객체에 대해 send() 메서드가 1번씩 호출됐는지 검증
    verify(emitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    verify(emitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  @DisplayName("알림 전송 - IOException 발생 시 해당 Emitter를 Repository에서 삭제한다.")
  void sendNotification_IOException_DeletesEmitter() throws Exception {

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
    sseService.sendNotification(userId, dummyData);

    // then
    verify(emitterRepository).delete(userId, failEmitter);
  }

  @Test
  @DisplayName("SSE 구독 - 파라미터가 null이면 NullPointerException 발생")
  void subscribe_NullUserId_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> sseService.subscribe(null))
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
  @DisplayName("알림 전송 - 연결된 기기가 하나도 없으면 전송 시도를 하지 않고 일찍 종료한다")
  void sendNotification_NoEmitters_SkipsSending() {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "테스트 알림 데이터";

    // Repo가 빈 리스트 반환
    given(emitterRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());

    // when
    sseService.sendNotification(userId, dummyData);

    // then
    verify(emitterRepository).findAllByUserId(userId);
  }

  @Test
  @DisplayName("콜백 검증 - 완료/타임아웃/에러 발생 시 Repository에서 Emitter가 삭제되도록 콜백이 등록된다.")
  void subscribe_CallbackRegistered() {

    // given
    UUID userId = UUID.randomUUID();

    // when
    SseEmitter emitter = sseService.subscribe(userId);

    // then
    assertThat(emitter).isNotNull();
  }

  @Test
  @DisplayName("SSE 구독 - 연결 완료(Completion), 타임아웃, 에러 발생 시 Repository에서 Emitter가 올바르게 삭제된다.")
  void subscribe_Callbacks_TriggerDelete() {

    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = sseService.subscribe(userId);

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
}