package com.mopl.mopl.global.exception;

public interface ErrorCode {
    int getNumeric();               // 1001
    String getDomain();             // EMPL
    String getErrorKey();           // NOT_FOUND
    String getCode();               // EMPL-NOT_FOUND
    String getMessage();            // 직원을 찾을 수 없습니다.
}
