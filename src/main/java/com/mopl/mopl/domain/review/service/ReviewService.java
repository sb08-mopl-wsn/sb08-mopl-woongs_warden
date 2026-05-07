package com.mopl.mopl.domain.review.service;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import java.util.UUID;

public interface ReviewService {

  Review createReview(ReviewCreateRequest request, User user);
  Review findReviewById(UUID reviewId);
  Review updateReview(UUID reviewId, ReviewUpdateRequest request, User user);
  void deleteReview(UUID reviewId, User user);
}
