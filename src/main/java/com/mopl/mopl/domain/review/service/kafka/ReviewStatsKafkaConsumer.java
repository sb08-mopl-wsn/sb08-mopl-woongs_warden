package com.mopl.mopl.domain.review.service.kafka;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.review.dto.response.ReviewStatsDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewNotFoundException;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewStatsKafkaConsumer {

  private final ContentRepository contentRepository;
  private final ReviewRepository reviewRepository;

  @Transactional
  @KafkaListener(topics = "review-created-topic", groupId = "mopl-review-stats-group")
  public void consumeReviewEventForStats(ReviewCreatedEvent event) {
    log.info("[Kafka Consumer] 별점 통계 업데이트 이벤트 수신 - reviewId: {}", event.reviewId());

    Review review = reviewRepository.findById(event.reviewId())
        .orElseThrow(() -> new ReviewNotFoundException(event.reviewId()));

    UUID contentId = review.getContent().getId();

    updateContentReviewStats(contentId);
  }

  private void updateContentReviewStats(UUID contentId) {
    Content content = contentRepository.findByIdForUpdate(contentId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    ReviewStatsDto stats = reviewRepository.getReviewStats(contentId);

    BigDecimal newAvgRating = BigDecimal.valueOf(stats.averageRating())
        .setScale(1, RoundingMode.HALF_UP);
    int newReviewCount = stats.reviewCount().intValue();

    content.updateReviewStats(newAvgRating, newReviewCount);
    log.info("콘텐츠 리뷰 통계 업데이트 완료: contentId={}", contentId);
  }
}