package com.mopl.mopl.domain.notification.controller;

import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "알림(Notification)", description = "글로벌 알림 내역 API")
public interface NotificationApi {

    @Operation(summary = "알림 목록 조회 (커서 페이지네이션)", description = "API 요청자의 알림 목록만 조회할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CursorResponseNotificationDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 페이징 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CursorResponseNotificationDto> getNotifications(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @ParameterObject @Valid @ModelAttribute CursorPaginationRequest request
    );

    @Operation(summary = "알림 읽음 처리", description = "특정 알림 단건을 삭제/읽음 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "성공 (응답 데이터 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류 (본인의 알림이 아님)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteNotification(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "삭제할 알림 UUID", required = true)
            @PathVariable("notificationId") UUID notificationId
    );
}