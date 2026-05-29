package com.mopl.mopl.domain.review.kafka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.service.kafka.ReviewKafkaProducer;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewKafkaProducer Unit Test")
class ReviewKafkaProducerTest {

  @InjectMocks
  private ReviewKafkaProducer reviewKafkaProducer;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Test
  @DisplayName("리뷰 작성 이벤트 발생 시 리뷰 전용 토픽으로 메시지를 전송한다.")
  void produceReviewEvent_SendsToKafka() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID writerId = UUID.randomUUID();

    User writer = User.builder().name("작성자").build();
    ReflectionTestUtils.setField(writer, "id", writerId);

    Review review = Review.builder().user(writer).build();
    ReflectionTestUtils.setField(review, "id", reviewId);

    ReviewCreatedEvent event = ReviewCreatedEvent.of(review);

    // when
    reviewKafkaProducer.produceReviewEvent(event);

    // then
    verify(kafkaTemplate).send(eq("review-created-topic"), eq(reviewId.toString()), eq(event));
  }
}