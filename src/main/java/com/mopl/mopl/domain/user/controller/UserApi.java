package com.mopl.mopl.domain.user.controller;

import com.mopl.mopl.domain.user.dto.CursorResponseUserDto;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @Operation(summary = "사용자 생성", description = "신규 사용자를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자")
    })
    ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request
    );

    @Operation(summary = "사용자 목록 조회", description = "커서 기반 사용자 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ResponseEntity<CursorResponseUserDto> getAllUsers(
            @Valid @ModelAttribute CursorUserRequest request
    );

    @Operation(summary = "사용자 상세 조회", description = "사용자 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<UserDto> getDetailUser(
            @Parameter(description = "사용자 ID")
            UUID userId
    );

    @Operation(summary = "사용자 권한 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<UserDto> updateUserRole(
            UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    );

    @Operation(summary = "비밀번호 변경")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<UserDto> updateUserPassword(
            UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    );

    @Operation(summary = "계정 잠금 상태 변경")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<UserDto> updateUserLocked(
            UUID userId,
            @Valid @RequestBody UserLockUpdateRequest request
    );

    @Operation(summary = "프로필 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<UserDto> updateProfile(
            UUID userId,
            @RequestPart("request") @Valid UserUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile profile
    );

    @Operation(summary = "현재 시청 세션 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<WatchingSessionDto> findCurrentWatchingSession(
            UUID watcherId,
            MoplUserDetails userDetails
    );
}