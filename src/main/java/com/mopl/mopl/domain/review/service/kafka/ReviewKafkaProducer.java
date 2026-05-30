package com.mopl.mopl.domain.review.service.kafka;

import com.mopl.mopl.global.event.ReviewCreatedEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceReviewEvent(ReviewCreatedEvent event) {
    log.info("[Kafka Producer] 리뷰 생성 이벤트 발행 - reviewId: {}", event.reviewId());

    String topic = "review-created-topic";
    CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
        topic,
        event.reviewId().toString(),
        event
    );

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("[Kafka Producer] \"{}\" 토픽 발행 실패 - reviewId: {}, error: {}",
            topic, event.reviewId(), ex.getMessage());
      } else {
        log.info("[Kafka Producer] \"{}\" 토픽 발행 성공 - reviewId: {}, offset: {}",
            topic, event.reviewId(), result.getRecordMetadata().offset());
      }
    });
  }
}