package com.mopl.mopl.global.event.kafka;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSecurityEventKafkaPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishUserEvent(String eventType, UUID userId, String email, String name, String detail) {
    publish(UserSecurityKafkaTopics.USER_EVENTS, eventType, userId, email, name, detail);
  }

  public void publishSecurityEvent(String eventType, UUID userId, String email, String detail) {
    publish(UserSecurityKafkaTopics.SECURITY_EVENTS, eventType, userId, email, null, detail);
  }

  private void publish(String topic, String eventType, UUID userId, String email, String name, String detail) {
    UserSecurityEventMessage message = new UserSecurityEventMessage(
        eventType,
        userId,
        email,
        name,
        detail,
        Instant.now()
    );

    String key = userId != null ? userId.toString() : "anonymous";
    kafkaTemplate.send(topic, key, message)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.warn("Kafka publish failed - topic: {}, eventType: {}, userId: {}", topic, eventType, userId, ex);
            return;
          }

          if (result != null) {
            log.debug("Kafka publish success - topic: {}, partition: {}, offset: {}, eventType: {}, userId: {}",
                topic,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                eventType,
                userId);
          }
        });
  }
}
