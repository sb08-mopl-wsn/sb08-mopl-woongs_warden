package com.mopl.mopl.domain.conversation.exception;

public class ConversationAccessDeniedException extends ConversationException {
  public ConversationAccessDeniedException() {
    super(ConversationErrorCode.CONVERSATION_ACCESS_DENIED);
  }

  public ConversationAccessDeniedException(String message) {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND);
  }

}
