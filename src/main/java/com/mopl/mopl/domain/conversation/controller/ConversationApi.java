package com.mopl.mopl.domain.conversation.controller;

import com.mopl.mopl.domain.conversation.dto.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "대화방(Conversation)", description = "1:1 대화방 관리 API")
public interface ConversationApi {

    @Operation(summary = "대화 생성", description = "상대방과의 1:1 대화방을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConversationDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationDto> createConversation(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Valid @RequestBody ConversationCreateRequest request
    );

    @Operation(summary = "대화 목록 조회 (커서 페이지네이션)", description = "API 요청자 본인의 대화 목록만 조회할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CursorResponseConversationDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 페이징 요청",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CursorResponseConversationDto> getMyConversations(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @ParameterObject @Valid @ModelAttribute CursorPaginationRequest request
    );

    @Operation(summary = "대화 조회", description = "대화방 ID로 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConversationDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (참여자가 아님)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화방을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationDto> getConversation(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "대화방 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @PathVariable("conversationId") UUID conversationId
    );

    @Operation(summary = "특정 사용자와의 대화 조회", description = "특정 유저와의 1:1 대화방이 존재하는지 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConversationDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "대화방을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ConversationDto> getConversationWith(
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "상대방 유저 UUID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
            @RequestParam("userId") UUID withUserId
    );
}