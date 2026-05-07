package com.mopl.mopl.domain.follow.exception;

import java.util.UUID;

public class FollowNotFoundException extends FollowException{

  public FollowNotFoundException() {
    super(FollowErrorCode.FOLLOW_NOT_FOUND);
  }

  public FollowNotFoundException(UUID followId) {
    super(FollowErrorCode.FOLLOW_NOT_FOUND, "팔로우 관계를 찾을 수 없습니다. id=" + followId);
  }
}
