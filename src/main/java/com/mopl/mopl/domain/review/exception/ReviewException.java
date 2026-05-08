package com.mopl.mopl.domain.review.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class ReviewException extends BusinessException {

  public ReviewException(ReviewErrorCode errorCode) {
    super(errorCode);
  }
  public ReviewException(ReviewErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
