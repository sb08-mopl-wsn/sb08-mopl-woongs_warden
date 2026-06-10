package com.mopl.mopl.domain.follow.controller;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "팔로우(Follow)", description = "팔로우 관련 API")
public interface FollowApi {

    @Operation(summary = "팔로우", description = "특정 유저를 팔로우합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "생성 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FollowDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신 팔로우 등)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 오류 (토큰 없음)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "대상 유저를 찾을 수 없음",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FollowDto> follow(
        @Parameter(hidden = true) MoplUserDetails userDetails,
        @Valid @RequestBody FollowRequest request
    );

    @Operation(summary = "팔로우 취소", description = "API 요청자 본인의 팔로우만 취소할 수 있습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "성공 (응답 데이터 없음)"),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "팔로우 내역을 찾을 수 없음",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> unfollow(
        @Parameter(hidden = true) MoplUserDetails userDetails,
        @Parameter(description = "언팔로우 할 상대방의 유저 ID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
        @PathVariable("followeeId") UUID followeeId
    );

    @Operation(summary = "특정 유저를 내가 팔로우하는지 여부 조회", description = "내가 해당 유저를 팔로우하고 있다면 팔로우 정보(FollowDto)를 반환하고, 아니면 404 Not Found를 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "팔로우 중임",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FollowDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 오류",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "팔로우하지 않음",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<FollowDto> isFollowedByMe(
        @Parameter(hidden = true) MoplUserDetails userDetails,
        @Parameter(description = "조회할 대상 유저 ID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
        @RequestParam("followeeId") UUID followeeId
    );

    @Operation(summary = "특정 유저의 팔로워 수 조회", description = "특정 유저를 팔로우하고 있는 사람들의 총 숫자를 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "404", description = "대상 유저를 찾을 수 없음",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Long> getFollowerCount(
        @Parameter(description = "조회할 대상 유저 ID", required = true, example = "01932a4b-1234-7abc-8def-0123456789ab")
        @RequestParam("followeeId") UUID followeeId
    );
}