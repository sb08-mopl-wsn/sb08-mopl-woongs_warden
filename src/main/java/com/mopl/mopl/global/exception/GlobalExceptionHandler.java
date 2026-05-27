package com.mopl.mopl.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String INCORRECT_PARAMETER = "'%s' 파라미터 값이 올바르지 않습니다. (허용된 값: %s)";
    private static final String INVALID_PARAMETER_FORMAT = "'%s' 파라미터의 형식이 올바르지 않습니다.";
    private static final String INVALID_REQUEST = "잘못된 접근입니다.";
    private static final String MISSING_PARAMETER = "'%s' 필수 파라미터가 누락되었습니다.";


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {

        ErrorCode code = e.getErrorCode();

        // 서버 로그 남기기
        log.error("비즈니스 예외 발생 : code={}, message={}, path={}", code.getCode(), code.getMessage(), request.getRequestURI());


        // 응답 생성
        ErrorResponse response;
        if (e.getHint() != null) {
            response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI(), e.getHint());
        } else {
            response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        }

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // @Valid 유효성 검사 실패 시 실행
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        // 에러 필드 정보 추출
        List<ErrorResponse.Detail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.Detail(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .toList();

        log.error("유효성 검사 실패 : code={}, message={}, path={}, details={}", code.getCode(), code.getMessage(), request.getRequestURI(), details);

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        String detailsMessage;
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            String allowedValues = Arrays.toString(e.getRequiredType().getEnumConstants());
            detailsMessage = String.format(INCORRECT_PARAMETER, e.getName(), allowedValues);
        } else {
            detailsMessage = String.format(INVALID_PARAMETER_FORMAT, e.getName());
        }

        // details 필드에 넣기 위해 리스트로 변환
        List<ErrorResponse.Detail> details = List.of(new ErrorResponse.Detail(e.getName(), detailsMessage, e.getValue()));

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // 파라미터 누락 예외
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        String detailsMessage = String.format(MISSING_PARAMETER, e.getParameterName());

        // ErrorResponse.Detail 객체로 포장
        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getParameterName(), detailsMessage, "null")
        );

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);

        log.error("필수 파라미터 누락 : code={}, message={}, path={}, details={}",
                code.getCode(), code.getMessage(), request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // 헤더가 없을 경우 발생하는 예외
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e, HttpServletRequest request) {

        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        String headerName = e.getHeaderName();
        String errorMessage = String.format("필수 헤더 '%s'가 누락되었습니다.", headerName);

        log.warn("필수 헤더 누락: code={}, message={}, path={}",
                code.getCode(), errorMessage, request.getRequestURI());

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(headerName, errorMessage, null)
        );

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // 시큐리티 - 인증 실패 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.UNAUTHORIZED;
        log.warn("인증 예외 발생: code={}, message={}, path={}",
            code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // 시큐리티 - 인가/권한 실패 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.FORBIDDEN;
        log.warn("인가(권한) 예외 발생 : code={}, message={}, path={}",
            code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // 그 외 모든 예외 처리 (최상위 예외 처리)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        // 로그로 상세 에러 기록
        log.error("서버 내부 오류 발생 : code={}, message={}, path={}",
                code.getCode(), e.getMessage(), request.getRequestURI(), e);

        // 클라이언트에 리턴해줄 오류 메시지 응답 생성
        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }
}