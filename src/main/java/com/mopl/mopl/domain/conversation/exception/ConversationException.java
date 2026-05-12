package com.mopl.mopl.domain.conversation.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class ConversationException extends BusinessException {
  public ConversationException(ConversationErrorCode errorCode) {
    super(errorCode);
  }
}
