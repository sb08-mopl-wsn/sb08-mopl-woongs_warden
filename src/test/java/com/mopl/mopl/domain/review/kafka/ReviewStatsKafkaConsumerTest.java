package com.mopl.mopl.domain.review.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.review.dto.response.ReviewStatsDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewNotFoundException;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import com.mopl.mopl.domain.review.service.kafka.ReviewStatsKafkaConsumer;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewStatsKafkaConsumer Unit Test")
class ReviewStatsKafkaConsumerTest {

  @InjectMocks
  private ReviewStatsKafkaConsumer reviewStatsKafkaConsumer;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Test
  @DisplayName("리뷰 생성 이벤트를 수신하면 콘텐츠의 평균 별점과 리뷰 개수를 업데이트한다.")
  void consumeReviewEventForStats_UpdatesContentStats() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    UUID writerId = UUID.randomUUID();

    User writer = User.builder().name("작성자").build();
    ReflectionTestUtils.setField(writer, "id", writerId);

    Content content = Content.builder().title("테스트 콘텐츠").build();
    ReflectionTestUtils.setField(content, "id", contentId);
    ReflectionTestUtils.setField(content, "avgRating", BigDecimal.ZERO);
    ReflectionTestUtils.setField(content, "reviewCount", 0);

    Review review = Review.builder().user(writer).content(content).rating(4.0).build();
    ReflectionTestUtils.setField(review, "id", reviewId);

    ReviewCreatedEvent event = ReviewCreatedEvent.of(review);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(contentRepository.findByIdForUpdate(contentId)).willReturn(Optional.of(content));

    // 새 평균 4.5점, 리뷰 10개라고 가정
    ReviewStatsDto mockStats = new ReviewStatsDto(10L, 4.5);
    given(reviewRepository.getReviewStats(contentId)).willReturn(mockStats);

    // when
    reviewStatsKafkaConsumer.consumeReviewEventForStats(event);

    // then
    verify(reviewRepository).findById(reviewId);
    verify(contentRepository).findByIdForUpdate(contentId);
    verify(reviewRepository).getReviewStats(contentId);

    assertThat(content.getAvgRating()).isEqualByComparingTo(new BigDecimal("4.5"));
    assertThat(content.getReviewCount()).isEqualTo(10);
  }

  @Test
  @DisplayName("존재하지 않는 리뷰 ID로 이벤트가 발생하면 ReviewNotFoundException을 던진다.")
  void consumeReviewEvent_WithNonExistentReview_ThrowsReviewNotFoundException() {
    // given
    UUID reviewId = UUID.randomUUID();
    ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, UUID.randomUUID(), "작성자");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewStatsKafkaConsumer.consumeReviewEventForStats(event))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 ID로 이벤트가 발생하면 ContentNotFoundException을 던진다.")
  void consumeReviewEvent_WithNonExistentContent_ThrowsContentNotFoundException() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, UUID.randomUUID(), "작성자");

    Content mockContent = Content.builder().build();
    ReflectionTestUtils.setField(mockContent, "id", contentId);
    Review mockReview = Review.builder().content(mockContent).build();
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));

    given(contentRepository.findByIdForUpdate(contentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewStatsKafkaConsumer.consumeReviewEventForStats(event))
        .isInstanceOf(ContentNotFoundException.class);
  }
}