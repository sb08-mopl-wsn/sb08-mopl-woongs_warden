package com.mopl.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT(9001, "INVALID_INPUT", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    FILE_UPLOAD_FAILED(9002, "FILE_UPLOAD_FAILED", HttpStatus.BAD_REQUEST, "파일 업로드에 실패했습니다."),
    SSE_CONNECTION_FAILED(9003, "SSE_CONNECTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "SSE 연결 초기화 중 에러가 발생했습니다."),
    UNAUTHORIZED(9004, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증 정보가 필요하거나 유효하지 않습니다."),
    FORBIDDEN(9005, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다."),
    CDN_URL_NOT_FOUND(9006, "CDN_URL_NOT_FOUND", HttpStatus.NOT_FOUND, "CDN 경로가 없습니다."),
    MAIL_LOAD_FAILED(9007, "MAIL_LOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "메일 생성에 실패했습니다."),
    MAIL_SEND_FAILED(9008, "MAIL_SEND_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다."),
    OAUTH2_PRINCIPAL_NOT_MATCH(9009, "OAUTH2_PRINCIPAL_NOT_MATCH", HttpStatus.UNAUTHORIZED, "유저 디테일이 일치하지 않습니다."),
    OAUTH2_TOKEN_GENERATION_FAILED(9010, "OAUTH2_TOKEN_GENERATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 토큰 생성에 실패했습니다."),

    OAUTH2_UNSUPPORTED_PROVIDER(9011, "OAUTH2_UNSUPPORTED_PROVIDER", HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    OAUTH2_EMAIL_NOT_VERIFIED(9012, "OAUTH2_EMAIL_NOT_VERIFIED", HttpStatus.UNAUTHORIZED, "구글 이메일 인증이 완료되지 않은 계정입니다."),
    OAUTH2_ALREADY_LINKED_OTHER_SOCIAL(9013, "OAUTH2_ALREADY_LINKED_OTHER_SOCIAL", HttpStatus.CONFLICT, "이미 다른 소셜 계정으로 가입된 이메일입니다."),
    OAUTH2_SOCIAL_ID_MISMATCH(9014, "OAUTH2_SOCIAL_ID_MISMATCH", HttpStatus.CONFLICT, "기존 구글 계정 정보와 일치하지 않습니다."),
    OAUTH2_MISSING_ATTRIBUTE(9015, "OAUTH2_MISSING_ATTRIBUTE", HttpStatus.BAD_REQUEST, "OAuth2 사용자 정보에 필수 값이 없습니다."),

    INTERNAL_SERVER_ERROR(9999, "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "GLOBAL";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}