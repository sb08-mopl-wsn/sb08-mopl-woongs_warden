package com.mopl.mopl.domain.review.service.kafka;

import com.mopl.mopl.global.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    log.info("[Kafka Producer] 별점 업데이트를 위한 ReviewEvent 카프카 발행 - reviewId: {}", event.reviewId());

    kafkaTemplate.send("review-created-topic", event.reviewId().toString(), event);
  }
}