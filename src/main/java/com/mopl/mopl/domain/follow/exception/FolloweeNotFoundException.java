package com.mopl.mopl.domain.follow.exception;

public class FolloweeNotFoundException extends FollowException {

  public FolloweeNotFoundException() {
    super(FollowErrorCode.FOLLOWEE_NOT_FOUND);
  }
}
