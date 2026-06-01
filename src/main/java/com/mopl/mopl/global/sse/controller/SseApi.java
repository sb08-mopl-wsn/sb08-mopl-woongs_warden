package com.mopl.mopl.global.sse.controller;

import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "실시간 통신(SSE)", description = "Server-Sent Events 연결 API")
public interface SseApi {

    @Operation(summary = "SSE 파이프 연결(구독)", description = "클라이언트와 서버 간의 단방향 실시간 알림 파이프를 연결합니다. (Last-Event-ID 헤더 포함 시 유실된 알림 복구 전송)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "연결 성공 (text/event-stream 반환)", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "서버 타임아웃 또는 연결 초과",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    SseEmitter subscribe(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "재접속 시 유실된 알림을 복구하기 위한 마지막 이벤트 ID", required = false)
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId
    );
}