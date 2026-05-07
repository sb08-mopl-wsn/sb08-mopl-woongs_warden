package com.mopl.mopl.domain.follow.exception;

public class SelfFollowException extends FollowException{

  public SelfFollowException() {
    super(FollowErrorCode.SELF_FOLLOW_NOT_ALLOWED);
  }
}
