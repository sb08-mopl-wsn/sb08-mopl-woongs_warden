package com.mopl.mopl.domain.follow.exception;

public class FollowerNotFoundException extends FollowException {

  public FollowerNotFoundException() {
    super(FollowErrorCode.FOLLOWEE_NOT_FOUND);
  }
}
