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

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto userDto = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getDetailUser(@PathVariable UUID userId) {
        UserDto userDto = userService.getUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        UserDto userDto = userService.updateUserRole(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }


    @PatchMapping("/{userId}/password")
    public ResponseEntity<UserDto> updateUserPassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UserDto userDto = userService.updateUserPassword(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PatchMapping("/{userId}/locked")
    public ResponseEntity<UserDto> updateUserLocked(
            @PathVariable UUID userId,
            @Valid @RequestBody UserLockUpdateRequest request
    ) {
        UserDto userDto = userService.updateUserLocked(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }
}