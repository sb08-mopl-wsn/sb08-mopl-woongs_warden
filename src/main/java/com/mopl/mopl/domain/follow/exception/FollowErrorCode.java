package com.mopl.mopl.domain.follow.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {

  FOLLOW_NOT_FOUND(3001, "NOT_FOUND", HttpStatus.NOT_FOUND, "팔로우 관계를 찾을 수 없습니다."),
  SELF_FOLLOW_NOT_ALLOWED(3002, "INVALID_REQUEST", HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
  // TODO: 추후 유저 관련 exception이 추가되면, 삭제 후 USER쪽에서 만든 exception으로 교체 필요
  USER_NOT_FOUND(3003, "USER_NOT_FOUND", HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() { return "FOLLOW"; }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
