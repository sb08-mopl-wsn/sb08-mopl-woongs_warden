package com.mopl.mopl.domain.review.service.impl;

import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
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
  public ReviewDto createReview(ReviewCreateRequest request, User user) {
    // TODO: 리뷰 생성 로직 작성해야함
    return null;
  }

  @Override
  public ReviewDto findReviewById(UUID reviewId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    return reviewMapper.toDto(review);
  }

  @Override
  @Transactional
  public ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, User user) {
    Review reviewToUpdate = getReviewAndCheckPermission(reviewId, user);

    reviewToUpdate.update(request.text(), request.rating());

    return reviewMapper.toDto(reviewToUpdate);
  }

  @Override
  @Transactional
  public void deleteReview(UUID reviewId, User user) {
    Review reviewToDelete = getReviewAndCheckPermission(reviewId, user);

    reviewRepository.delete(reviewToDelete);
  }

  /**
   * 리뷰를 ID로 조회, 현재 사용자가 해당 리뷰의 작성자인지 확인
   * @throws ReviewNotFoundException 리뷰를 찾을 수 없는 경우
   * @throws ReviewException         현재 사용자가 리뷰의 작성자가 아닌 경우 (FORBIDDEN)
   */
  private Review getReviewAndCheckPermission(UUID reviewId, User user) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException(reviewId));

    if (!review.getUser().getId().equals(user.getId())) {
      throw new ReviewException(ReviewErrorCode.FORBIDDEN);
    }
    return review;
  }
}