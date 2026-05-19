package com.mopl.mopl.domain.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends ReviewException {

  public ReviewNotFoundException() {
    super(ReviewErrorCode.REVIEW_NOT_FOUND);
  }

  public ReviewNotFoundException(UUID reviewId) {
    super(ReviewErrorCode.REVIEW_NOT_FOUND, ReviewErrorCode.REVIEW_NOT_FOUND.getMessage() + " id=" + reviewId);
  }
}