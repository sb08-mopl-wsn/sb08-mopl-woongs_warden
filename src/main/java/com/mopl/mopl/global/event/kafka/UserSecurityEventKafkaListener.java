package com.mopl.mopl.global.event.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserSecurityEventKafkaListener {

  @KafkaListener(topics = UserSecurityKafkaTopics.USER_EVENTS, groupId = "mopl-user-events")
  public void onUserEvent(UserSecurityEventMessage message) {
    log.info("[Kafka][USER] type={}, userId={}, name={}, detail={}, occurredAt={}",
        message.eventType(), message.userId(), message.name(), message.detail(), message.occurredAt());
  }

  @KafkaListener(topics = UserSecurityKafkaTopics.SECURITY_EVENTS, groupId = "mopl-security-events")
  public void onSecurityEvent(UserSecurityEventMessage message) {
    log.info("[Kafka][SECURITY] type={}, userId={}, email={}, detail={}, occurredAt={}",
        message.eventType(), message.userId(), message.email(), message.detail(), message.occurredAt());
  }
}
