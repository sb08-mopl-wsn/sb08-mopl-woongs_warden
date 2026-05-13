package com.mopl.mopl.domain.review.exception;

public class ReviewCursorException extends ReviewException {

  public ReviewCursorException() {
    super(ReviewErrorCode.REVIEW_INVALID_CURSOR);
  }

  public ReviewCursorException(String message) {
    super(ReviewErrorCode.REVIEW_INVALID_CURSOR, message);
  }
}
