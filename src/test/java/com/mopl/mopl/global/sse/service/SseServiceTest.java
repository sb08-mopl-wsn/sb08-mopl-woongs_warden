package com.mopl.mopl.global.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.global.sse.repository.SseEmitterRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @InjectMocks
  private SseService sseService;

  @Mock
  private SseEmitterRepository emitterRepository;

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
    assertThat(result.getTimeout()).isEqualTo(30L * 1000 * 60);
  }

  @Test
  @DisplayName("알림 전송 - 유저의 모든 다중 기기 Emitter를 찾아 전송을 시도한다.")
  void sendNotification_Success() {

    // given
    UUID userId = UUID.randomUUID();
    Object dummyData = "테스트 알림 데이터";

    // 가짜 Emitter 객체 2개 생성 (다중 기기 접속 상황)
    SseEmitter emitter1 = new SseEmitter();
    SseEmitter emitter2 = new SseEmitter();
    List<SseEmitter> emitters = new CopyOnWriteArrayList<>(List.of(emitter1, emitter2));

    given(emitterRepository.findAllByUserId(userId)).willReturn(emitters);

    // when
    sseService.sendNotification(userId, dummyData);

    // then
    verify(emitterRepository).findAllByUserId(userId);
  }

}