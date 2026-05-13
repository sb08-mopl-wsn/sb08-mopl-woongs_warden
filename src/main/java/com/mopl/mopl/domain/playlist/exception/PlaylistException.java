package com.mopl.mopl.domain.playlist.exception;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.ErrorCode;

public class PlaylistException extends BusinessException {

  public PlaylistException(ErrorCode errorCode) {
    super(errorCode);
  }
  public PlaylistException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}