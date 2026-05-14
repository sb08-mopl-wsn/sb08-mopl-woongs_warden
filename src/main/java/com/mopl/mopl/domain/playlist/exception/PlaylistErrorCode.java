package com.mopl.mopl.domain.playlist.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

  PLAYLIST_NOT_FOUND(5001, "NOT_FOUND", HttpStatus.NOT_FOUND, "해당 플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_FORBIDDEN(5002, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 플레이리스트에 대한 권한이 없습니다."),
  CONTENT_ALREADY_IN_PLAYLIST(5003, "CONFLICT", HttpStatus.CONFLICT, "이미 플레이리스트에 추가된 콘텐츠입니다."),
  CONTENT_NOT_FOUND_IN_PLAYLIST(5004, "NOT_FOUND", HttpStatus.NOT_FOUND, "플레이리스트에 해당 콘텐츠가 존재하지 않습니다."),
  DUPLICATE_SUBSCRIPTION(5005, "CONFLICT", HttpStatus.CONFLICT, "이미 구독한 플레이리스트입니다."),
  SUBSCRIPTION_NOT_FOUND(5006, "NOT_FOUND", HttpStatus.NOT_FOUND, "구독하지 않은 플레이리스트입니다."),
  PLAYLIST_INVALID_CURSOR(5007, "INVALID_CURSOR", HttpStatus.BAD_REQUEST, "유효하지 않은 커서 값입니다.");


  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "PLAYLIST";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}