package com.mopl.mopl.domain.user.exception;

import java.util.UUID;

public class UserNotFoundException extends UserException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(UUID id) {
        super(UserErrorCode.USER_NOT_FOUND,
                "해당 사용자를 찾을 수 없습니다 - id: " + id);
    }

    public UserNotFoundException(String name) {
        super(UserErrorCode.USER_NOT_FOUND,
                "해당 사용자를 찾을 수 없습니다 - 이름: " + name);
    }
}