package com.mopl.mopl.domain.content.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Content 예외 및 에러코드 단위 테스트")
class ContentExceptionTest {

    @Nested
    @DisplayName("ContentErrorCode - 에러코드 값 검증")
    class ContentErrorCodeTest {

        @Test
        @DisplayName("CONTENT_NOT_FOUND 에러코드는 도메인이 CONT이고 HTTP 상태가 404이다.")
        void givenContentNotFound_whenGetCodeProperties_thenCorrectValues() {
            ContentErrorCode code = ContentErrorCode.CONTENT_NOT_FOUND;

            assertThat(code.getDomain()).isEqualTo("CONT");
            assertThat(code.getCode()).isEqualTo("CONT-NOT_FOUND");
            assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(code.getNumeric()).isEqualTo(1001);
            assertThat(code.getMessage()).isNotBlank();
        }

        @Test
        @DisplayName("CONTENT_INVALID_TYPE 에러코드는 HTTP 상태가 400이다.")
        void givenContentInvalidType_whenGetHttpStatus_thenBadRequest() {
            ContentErrorCode code = ContentErrorCode.CONTENT_INVALID_TYPE;

            assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code.getCode()).isEqualTo("CONT-INVALID_TYPE");
            assertThat(code.getNumeric()).isEqualTo(1003);
        }

        @Test
        @DisplayName("CONTENT_INVALID_CURSOR 에러코드는 HTTP 상태가 400이다.")
        void givenContentInvalidCursor_whenGetHttpStatus_thenBadRequest() {
            ContentErrorCode code = ContentErrorCode.CONTENT_INVALID_CURSOR;

            assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(code.getCode()).isEqualTo("CONT-INVALID_CURSOR");
            assertThat(code.getNumeric()).isEqualTo(1004);
        }

        @Test
        @DisplayName("CONTENT_DUPLICATE 에러코드는 HTTP 상태가 409이다.")
        void givenContentDuplicate_whenGetHttpStatus_thenConflict() {
            ContentErrorCode code = ContentErrorCode.CONTENT_DUPLICATE;

            assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(code.getCode()).isEqualTo("CONT-DUPLICATE");
            assertThat(code.getNumeric()).isEqualTo(1002);
        }
    }

    @Nested
    @DisplayName("ContentNotFoundException - 예외 생성 및 메시지 검증")
    class ContentNotFoundExceptionTest {

        @Test
        @DisplayName("기본 생성자로 ContentNotFoundException을 생성하면 기본 메시지가 설정된다.")
        void givenNoArgs_whenCreateContentNotFoundException_thenDefaultMessage() {
            ContentNotFoundException ex = new ContentNotFoundException();

            assertThat(ex).isInstanceOf(ContentException.class);
            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("UUID와 함께 ContentNotFoundException을 생성하면 ID 포함 메시지가 설정된다.")
        void givenUuid_whenCreateContentNotFoundException_thenIdIncludedInMessage() {
            UUID id = UUID.randomUUID();

            ContentNotFoundException ex = new ContentNotFoundException(id);

            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
            assertThat(ex.getMessage()).contains(id.toString());
        }

        @Test
        @DisplayName("ContentNotFoundException은 ContentException의 하위 타입이다.")
        void givenContentNotFoundException_whenCheckHierarchy_thenIsContentException() {
            ContentNotFoundException ex = new ContentNotFoundException();

            assertThat(ex).isInstanceOf(ContentException.class);
            assertThat(ex).isInstanceOf(com.mopl.mopl.global.exception.BusinessException.class);
        }
    }

    @Nested
    @DisplayName("ContentCursorException - 예외 생성 검증")
    class ContentCursorExceptionTest {

        @Test
        @DisplayName("ContentCursorException 생성 시 CONTENT_INVALID_CURSOR 에러코드가 설정된다.")
        void givenNoArgs_whenCreateContentCursorException_thenCursorErrorCode() {
            ContentCursorException ex = new ContentCursorException();

            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_INVALID_CURSOR);
            assertThat(ex).isInstanceOf(ContentException.class);
        }
    }

    @Nested
    @DisplayName("ContentInvalidTypeException - 예외 생성 검증")
    class ContentInvalidTypeExceptionTest {

        @Test
        @DisplayName("기본 생성자로 ContentInvalidTypeException을 생성하면 CONTENT_INVALID_TYPE 에러코드가 설정된다.")
        void givenNoArgs_whenCreateContentInvalidTypeException_thenInvalidTypeErrorCode() {
            ContentInvalidTypeException ex = new ContentInvalidTypeException();

            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_INVALID_TYPE);
            assertThat(ex).isInstanceOf(ContentException.class);
        }

        @Test
        @DisplayName("타입 문자열과 함께 ContentInvalidTypeException을 생성하면 타입이 메시지에 포함된다.")
        void givenTypeString_whenCreateContentInvalidTypeException_thenTypeInMessage() {
            String invalidType = "anime";

            ContentInvalidTypeException ex = new ContentInvalidTypeException(invalidType);

            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_INVALID_TYPE);
            assertThat(ex.getMessage()).contains(invalidType);
        }
    }

    @Nested
    @DisplayName("ContentException - 기본 예외 생성 검증")
    class ContentExceptionBaseTest {

        @Test
        @DisplayName("에러코드만 지정하면 에러코드의 기본 메시지가 사용된다.")
        void givenErrorCode_whenCreateContentException_thenDefaultMessageUsed() {
            ContentException ex = new ContentException(ContentErrorCode.CONTENT_NOT_FOUND);

            assertThat(ex.getMessage()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND.getMessage());
            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
        }

        @Test
        @DisplayName("에러코드와 커스텀 메시지를 지정하면 커스텀 메시지가 사용된다.")
        void givenErrorCodeAndCustomMessage_whenCreateContentException_thenCustomMessageUsed() {
            String customMessage = "커스텀 에러 메시지";

            ContentException ex = new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, customMessage);

            assertThat(ex.getMessage()).isEqualTo(customMessage);
            assertThat(ex.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
        }
    }
}