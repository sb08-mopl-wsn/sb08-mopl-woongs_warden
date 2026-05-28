package com.mopl.mopl.global.redis.service;

import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  // SSE용 공용 채널
  public static final ChannelTopic SSE_TOPIC = new ChannelTopic("sse-topic");
  // WebSocket(DM)용 공용 채널
  public static final ChannelTopic WS_TOPIC = new ChannelTopic("ws-topic");

  public void publishSse(RedisPubMessage message) {
    log.debug("Redis에 SSE 메시지 발행 - targetUserId: {}", message.targetUserId());
    redisTemplate.convertAndSend(SSE_TOPIC.getTopic(), message);
  }

  public void publishWs(RedisPubMessage message) {
    log.debug("Redis에 WebSocket 메시지 발행 - targetUserId: {}", message.targetUserId());
    redisTemplate.convertAndSend(WS_TOPIC.getTopic(), message);
  }
}
