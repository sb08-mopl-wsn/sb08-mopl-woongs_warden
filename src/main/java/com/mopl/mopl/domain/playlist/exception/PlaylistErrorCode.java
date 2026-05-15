package com.mopl.mopl.domain.playlist.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {

  PLAYLIST_NOT_FOUND(4501, "NOT_FOUND", HttpStatus.NOT_FOUND, "해당 플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_FORBIDDEN(4502, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 플레이리스트에 대한 권한이 없습니다."),
  CONTENT_ALREADY_IN_PLAYLIST(4503, "DUPLICATE", HttpStatus.CONFLICT, "이미 플레이리스트에 추가된 콘텐츠입니다."),
  CONTENT_NOT_FOUND_IN_PLAYLIST(4504, "NOT_FOUND", HttpStatus.NOT_FOUND, "플레이리스트에 해당 콘텐츠가 존재하지 않습니다."),
  PLAYLIST_DUPLICATE_SUBSCRIPTION(4505, "DUPLICATE", HttpStatus.CONFLICT, "이미 구독한 플레이리스트입니다."),
  PLAYLIST_SUBSCRIPTION_NOT_FOUND(4506, "NOT_FOUND", HttpStatus.NOT_FOUND, "구독하지 않은 플레이리스트입니다."),
  PLAYLIST_INVALID_CURSOR(4507, "INVALID_CURSOR", HttpStatus.BAD_REQUEST, "유효하지 않은 커서 값입니다."),
  PLAYLIST_SELF_SUBSCRIPTION_NOT_ALLOWED(4508, "INVALID_REQUEST", HttpStatus.CONFLICT, "자신의 플레이리스트는 구독할 수 없습니다."),
  PLAYLIST_UPDATE_FAILED(4509, "UPDATE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "플레이리스트 정보 업데이트에 실패했습니다.");

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