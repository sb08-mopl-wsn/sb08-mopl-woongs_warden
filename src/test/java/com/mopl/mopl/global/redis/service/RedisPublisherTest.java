package com.mopl.mopl.global.redis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

  @InjectMocks
  private RedisPublisher redisPublisher;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Test
  @DisplayName("SSE 메시지를 발행하면 sse-topic 채널로 전송된다.")
  void publishSse_Success() {

    // given
    RedisPubMessage message = new RedisPubMessage(UUID.randomUUID(), "notifications", "test data");

    // when
    redisPublisher.publishSse(message);

    // then
    verify(redisTemplate, times(1)).convertAndSend(RedisPublisher.SSE_TOPIC.getTopic(), message);
  }

  @Test
  @DisplayName("WebSocket 메시지를 발행하면 ws-topic 채널로 전송된다.")
  void publishWs_Success() {

    // given
    RedisPubMessage message = new RedisPubMessage(UUID.randomUUID(), "/sub/test", "test data");

    // when
    redisPublisher.publishWs(message);

    // then
    verify(redisTemplate, times(1)).convertAndSend(RedisPublisher.WS_TOPIC.getTopic(), message);
  }

}