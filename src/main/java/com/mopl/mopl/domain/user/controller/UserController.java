package com.mopl.mopl.domain.user.controller;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;
import com.mopl.mopl.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping()
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("[User-Controller] 생성 요청 시작: content = {}", request.email());

        UserDto userDto = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getDetailUser(@PathVariable UUID userId) {
        log.info("[User-Controller] 조회 요청 시작: userId = {}", userId);

        UserDto userDto = userService.getUser(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userDto);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        log.info("[User-Controller] 수정 요청 시작: content = userId: {}", userId);

        UserDto userDto = userService.updateUserRole(userId, request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(userDto);
    }


    @PatchMapping("/{userId}/password")
    public ResponseEntity<UserDto> updateUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("[User-Controller] 수정 요청 시작: content = userId: {}", userId);

        UserDto userDto = userService.updateUserPassword(userId, request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(userDto);
    }

    @PatchMapping("/{userId}/locked")
    public ResponseEntity<UserDto> updateUserLocked(
            @PathVariable UUID userId,
            @Valid @RequestBody UserLockUpdateRequest request
    ) {
        log.info("[User-Controller] 수정 요청 시작: content = userId: {}", userId);

        UserDto userDto = userService.updateUserLocked(userId, request);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(userDto);
    }
}