package com.mopl.mopl.domain.conversation.exception;

import java.util.UUID;

public class ConversationNotFoundException extends ConversationException {
  public ConversationNotFoundException(UUID conversationId) {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND);
  }

  public ConversationNotFoundException(UUID conversationId, String message) {
    super(ConversationErrorCode.CONVERSATION_NOT_FOUND, message);
  }
}
