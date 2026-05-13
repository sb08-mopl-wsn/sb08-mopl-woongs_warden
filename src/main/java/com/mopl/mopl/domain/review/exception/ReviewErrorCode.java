package com.mopl.mopl.domain.review.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

  REVIEW_NOT_FOUND(4001, "NOT_FOUND", HttpStatus.NOT_FOUND, "해당 리뷰를 찾을 수 없습니다."),
  DUPLICATE_REVIEW(4002, "DUPLICATE", HttpStatus.CONFLICT, "이미 해당 콘텐츠에 대한 리뷰를 작성했습니다."),
  REVIEW_FORBIDDEN(4003, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 리뷰에 대한 권한이 없습니다."),
  INVALID_REVIEW_INPUT(4004, "INVALID_INPUT", HttpStatus.BAD_REQUEST, "리뷰 입력값이 유효하지 않습니다."),
  REVIEW_INVALID_CURSOR(4005, "INVALID_CURSOR", HttpStatus.BAD_REQUEST, "유효하지 않은 커서 값입니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "REVIEW";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}