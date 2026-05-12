package com.mopl.mopl.domain.review.service.impl;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewSearchRequest;
import com.mopl.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.review.exception.ReviewErrorCode;
import com.mopl.mopl.domain.review.exception.ReviewException;
import com.mopl.mopl.domain.review.exception.ReviewNotFoundException;
import com.mopl.mopl.domain.review.mapper.ReviewMapper;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import com.mopl.mopl.domain.review.service.ReviewService;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final ContentRepository contentRepository;
  private final UserRepository userRepository;
  private final ReviewMapper reviewMapper;

  @Override
  @Transactional
  public ReviewDto createReview(ReviewCreateRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    Content content = contentRepository.findById(request.contentId())
        .orElseThrow(() -> new ContentNotFoundException(request.contentId()));

    Review review = reviewMapper.toEntity(request, user, content);

    try {
      Review savedReview = reviewRepository.saveAndFlush(review);
      return reviewMapper.toDto(savedReview);
    } catch (DataIntegrityViolationException e) {
      throw new ReviewException(ReviewErrorCode.DUPLICATE_REVIEW);
    }
  }

  @Override
  public ReviewDto findReviewById(UUID reviewId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    return reviewMapper.toDto(review);
  }

  @Override
  public CursorResponseReviewDto findReviews(ReviewSearchRequest request) {
    Slice<Review> reviewSlice = reviewRepository.findReviews(request);

    List<ReviewDto> reviewDtos = reviewSlice.getContent().stream()
        .map(reviewMapper::toDto)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (reviewSlice.hasNext() && reviewSlice.hasContent()) {
      Review lastReview = reviewSlice.getContent().get(reviewSlice.getContent().size() - 1);
      nextCursor = extractCursor(lastReview, request.sortBy());
      nextIdAfter = lastReview.getId();
    }

    long totalCount = reviewRepository.countReviews(request.contentId());

    return new CursorResponseReviewDto(
        reviewDtos,
        nextCursor,
        nextIdAfter,
        reviewSlice.hasNext(),
        totalCount,
        request.sortBy(),
        request.sortDirection()
    );
  }

  @Override
  @Transactional
  public ReviewDto updateReview(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    Review reviewToUpdate = getReviewAndCheckPermission(reviewId, user);
    reviewToUpdate.update(request.text(), request.rating());
    return reviewMapper.toDto(reviewToUpdate);
  }

  @Override
  @Transactional
  public void deleteReview(UUID reviewId, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

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
      throw new ReviewException(ReviewErrorCode.REVIEW_FORBIDDEN);
    }
    return review;
  }

  private String extractCursor(Review review, String sortBy) {
    if ("rating".equalsIgnoreCase(sortBy)) {
      return String.valueOf(review.getRating());
    }
    // 기본값 createdAt
    return review.getCreatedAt().toString();
  }
}