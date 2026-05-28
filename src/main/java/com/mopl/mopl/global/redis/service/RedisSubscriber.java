package com.mopl.mopl.global.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.redis.dto.RedisPubMessage;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SseService sseService;
  private final SimpMessageSendingOperations messagingTemplate; // websocket 발송용

  @Override
  public void onMessage(Message message, byte[] pattern) {

    try {
      // Redis에서 받은 바이트 데이터를 문자열로 변환 (타입 강제 방지)
      String publishMessage = (String) redisTemplate.getStringSerializer().deserialize(message.getBody());
      String channel = new String(message.getChannel());

      // JSON 문자열을 DTO로 유연하게 역직렬화 (Object -> LinkedHashMap)
      RedisPubMessage pubMessage = objectMapper.readValue(publishMessage, RedisPubMessage.class);

      if (channel.equals(RedisPublisher.SSE_TOPIC.getTopic())) {
        // 내 서버 로컬에 이 유저가 물려있는지 확인 후, 있으면 SSE 발송
        sseService.sendToLocalClient(pubMessage.targetUserId(), pubMessage.eventName(), pubMessage.data());
      } else if (channel.equals(RedisPublisher.WS_TOPIC.getTopic())) {
        // 웹소켓 처리. 특정 유저에게 메시지 전송
        String destination = pubMessage.eventName();
        messagingTemplate.convertAndSend(destination, pubMessage.data());
      }
    } catch (Exception e) {
      log.error("Redis 메시지 수신 및 파싱 중 에러 발생", e);
    }
  }
}
