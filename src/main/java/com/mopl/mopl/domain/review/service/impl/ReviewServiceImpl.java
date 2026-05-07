package com.mopl.mopl.domain.review.service.impl;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewErrorCode;
import com.mopl.mopl.domain.review.exception.ReviewException;
import com.mopl.mopl.domain.review.exception.ReviewNotFoundException;
import com.mopl.mopl.domain.review.mapper.ReviewMapper;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import com.mopl.mopl.domain.review.service.ReviewService;
import com.mopl.mopl.domain.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  // private final ContentRepository contentRepository; // 최신화 되면 주석 제거
  private final ReviewMapper reviewMapper;

  @Override
  @Transactional
  public Review createReview(ReviewCreateRequest request, User user) {
    // TODO: 리뷰 생성 로직 작성해야함
    return null;
  }

  @Override
  public Review findReviewById(UUID reviewId) {
    return reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException(reviewId));
  }

  @Override
  @Transactional
  public Review updateReview(UUID reviewId, ReviewUpdateRequest request, User user) {
    Review review = findReviewById(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      throw new ReviewException(ReviewErrorCode.FORBIDDEN);
    }

    review.update(request.text(), request.rating());

    return review;
  }

  @Override
  @Transactional
  public void deleteReview(UUID reviewId, User user) {
    Review review = findReviewById(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      throw new ReviewException(ReviewErrorCode.FORBIDDEN);
    }

    reviewRepository.delete(review);
  }
}