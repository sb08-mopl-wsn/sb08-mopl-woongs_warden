package com.mopl.mopl.domain.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends ReviewException {

  public ReviewNotFoundException() {
    super(ReviewErrorCode.REVIEW_NOT_FOUND);
  }

  public ReviewNotFoundException(UUID reviewId) {
    super(ReviewErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다. id=" + reviewId);
  }
}