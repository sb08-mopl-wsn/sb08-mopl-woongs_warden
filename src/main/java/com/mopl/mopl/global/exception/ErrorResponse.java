package com.mopl.mopl.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


// 에러 응답용 DTO
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외
public record ErrorResponse(
        Instant timestamp,
        String traceId,
        String code,
        String errorKey,
        int numeric,
        String message, // 에러 제목 (예: 잘못된 요청입니다.)
        String details, // 에러 상세 내용 (예: 부서 코드는 필수입니다.) - 프론트엔드 팝업용
        List<Detail> fieldErrors, // 유효성 검사 에러 목록 (기존 details -> fieldErrors 변경)
        String hint
) {

    public record Detail(String field, String issue, Object rejected) {

    }

    // 1. 기본형 (BusinessException 등 일반적인 에러)
    public static ErrorResponse of(ErrorCode code, String message, String path) {
        return new ErrorResponse(
                Instant.now(),
                UUID.randomUUID().toString().substring(0, 8),
                code.getCode(),
                code.getErrorKey(),
                code.getNumeric(),
                code.getMessage(), // message에는 에러 코드의 기본 메시지(제목)를 넣음
                message,           // details에는 인자로 받은 상세 메시지를 넣음
                null,
                null
        );
    }

    // 2. Details 포함형 (유효성 검사 실패 등 상세 정보가 필요한 경우)
    public static ErrorResponse of(ErrorCode code, String message, String path,
                                   List<Detail> fieldErrors) {
        return new ErrorResponse(
                Instant.now(),
                UUID.randomUUID().toString().substring(0, 8),
                code.getCode(),
                code.getErrorKey(),
                code.getNumeric(),
                code.getMessage(), // message에는 에러 코드의 기본 메시지(제목)를 넣음
                message,           // details에는 인자로 받은 상세 메시지를 넣음
                fieldErrors,       // 유효성 검사 에러 목록
                null
        );
    }

    // 3. Hint 포함형 (사용자에게 힌트를 주고 싶은 경우)
    public static ErrorResponse of(ErrorCode code, String message, String path, String hint) {
        return new ErrorResponse(
                Instant.now(),
                UUID.randomUUID().toString().substring(0, 8),
                code.getCode(),
                code.getErrorKey(),
                code.getNumeric(),
                code.getMessage(), // message에는 에러 코드의 기본 메시지(제목)를 넣음
                message,           // details에는 인자로 받은 상세 메시지를 넣음
                null,
                hint
        );
    }
}
