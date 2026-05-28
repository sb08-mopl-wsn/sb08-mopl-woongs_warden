package com.mopl.mopl.global.redis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.sse.service.SseService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

  @InjectMocks
  private RedisSubscriber redisSubscriber;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private SseService sseService;

  @Mock
  private SimpMessageSendingOperations messagingTemplate;

  @Mock
  private RedisSerializer<String> stringSerializer;

  private UUID targetUserId;
  private RedisPubMessage pubMessage;
  private Message redisMessage;

  @BeforeEach
  void setUp() {
    targetUserId = UUID.randomUUID();
    pubMessage = new RedisPubMessage(targetUserId, "testEvent", "testData");
    redisMessage = mock(Message.class);

    given(redisTemplate.getStringSerializer()).willReturn(stringSerializer);
    given(stringSerializer.deserialize(any())).willReturn("mockedJsonString");
  }

  @Test
  @DisplayName("SSE 토픽으로 메시지가 들어오면 SseService를 호출하여 로컬 발송을 시도한다.")
  void onMessage_SseTopic_CallsSseService() throws Exception {

    // given
    byte[] sseChannel = RedisPublisher.SSE_TOPIC.getTopic().getBytes();
    given(redisMessage.getChannel()).willReturn(sseChannel);
    given(objectMapper.readValue("mockedJsonString", RedisPubMessage.class)).willReturn(pubMessage);

    // when
    redisSubscriber.onMessage(redisMessage, null);

    // then
    verify(sseService).sendToLocalClient(targetUserId, "testEvent", "testData");
    verify(messagingTemplate, never()).convertAndSend(Optional.ofNullable(any(String.class)), any());
  }

  @Test
  @DisplayName("WS 토픽으로 메시지가 들어오면 SimpMessagingTemplate을 호출하여 브로드캐스팅한다.")
  void onMessage_WsTopic_CallsMessagingTemplate() throws Exception {

    // given
    byte[] wsChannel = RedisPublisher.WS_TOPIC.getTopic().getBytes();
    given(redisMessage.getChannel()).willReturn(wsChannel);
    given(objectMapper.readValue("mockedJsonString", RedisPubMessage.class)).willReturn(pubMessage);

    // when
    redisSubscriber.onMessage(redisMessage, null);

    // then
    verify(messagingTemplate).convertAndSend("testEvent", "testData");
    verify(sseService, never()).sendToLocalClient(any(), any(), any());
  }

  @Test
  @DisplayName("알 수 없는 토픽이거나 예외가 발생하면 에러 로그를 남기고 정상 종료된다(예외를 던지지 않는다).")
  void onMessage_Exception_HandledGracefully() throws Exception {

    // given
    byte[] unknownChannel = "unknown-topic".getBytes();
    given(redisMessage.getChannel()).willReturn(unknownChannel);

    given(objectMapper.readValue("mockedJsonString", RedisPubMessage.class)).willThrow(new RuntimeException("파싱 에러"));

    // when
    redisSubscriber.onMessage(redisMessage, null);

    // then
    verify(sseService, never()).sendToLocalClient(any(), any(), any());
    verify(messagingTemplate, never()).convertAndSend(Optional.ofNullable(any(String.class)), any());
  }
}