package com.mopl.mopl.domain.review.exception;

public class InvalidReviewInputException extends ReviewException {

  public InvalidReviewInputException() {
    super(ReviewErrorCode.INVALID_REVIEW_INPUT);
  }

  public InvalidReviewInputException(String detailMessage) {
    super(ReviewErrorCode.INVALID_REVIEW_INPUT, "리뷰 입력값이 유효하지 않습니다: " + detailMessage);
  }
}
