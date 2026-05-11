package com.mopl.mopl.domain.review.exception;

public class ReviewCursorException extends ReviewException {

  public ReviewCursorException() {
    super(ReviewErrorCode.INVALID_CURSOR);
  }

  public ReviewCursorException(String message) {
    super(ReviewErrorCode.INVALID_CURSOR, message);
  }
}
