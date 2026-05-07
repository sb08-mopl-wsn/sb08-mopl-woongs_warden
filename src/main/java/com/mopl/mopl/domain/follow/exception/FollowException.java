package com.mopl.mopl.domain.follow.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class FollowException extends BusinessException {

  public FollowException(FollowErrorCode errorCode) {
    super(errorCode);
  }

  public FollowException(FollowErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
