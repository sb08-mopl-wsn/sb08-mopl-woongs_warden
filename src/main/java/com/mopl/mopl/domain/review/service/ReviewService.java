package com.mopl.mopl.domain.review.service;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.user.entity.User;
import java.util.UUID;

public interface ReviewService {

  ReviewDto createReview(ReviewCreateRequest request, User user);
  ReviewDto findReviewById(UUID reviewId);
  CursorResponseReviewDto findReviews(ReviewSearchRequest request);
  ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, User user);
  void deleteReview(UUID reviewId, User user);
}
