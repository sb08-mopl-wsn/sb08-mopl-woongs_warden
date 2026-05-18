package com.mopl.mopl.domain.review.repository;

import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.response.ReviewStatsDto;
import com.mopl.mopl.domain.review.entity.Review;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface ReviewRepositoryCustom {
  // 커서 기반 페이지네이션을 적용하여 리뷰 목록을 조회
  Slice<Review> findReviews(ReviewSearchRequest request);

  //특정 콘텐츠에 달린 전체 리뷰 개수를 조회
  long countReviews(UUID contentId);

  ReviewStatsDto getReviewStats(UUID contentId);
}