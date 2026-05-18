package com.mopl.mopl.domain.user.controller;

import com.mopl.mopl.domain.user.dto.CursorResponseUserDto;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;
import com.mopl.mopl.domain.user.service.UserService;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.service.WatchingSessionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Slf4j
public class UserController {
    private final UserService userService;
    private final WatchingSessionService watchingSessionService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserDto userDto = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @GetMapping
    public ResponseEntity<CursorResponseUserDto> getAllUsers(
            @Valid @ModelAttribute CursorUserRequest request
    ) {
        CursorResponseUserDto response = userService.getAllUsers(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
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

    @PatchMapping(
            path = "{userId}",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable("userId") UUID userId,
            @RequestPart("request") @Valid UserUpdateRequest userUpdateRequest,
            @RequestPart(value = "image", required = false) MultipartFile profile
    ) {
        UserDto updatedUser = userService.updateProfile(userId, userUpdateRequest, profile);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedUser);
    }

    @GetMapping("/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> findCurrentWatchingSession(
            @PathVariable UUID watcherId,
            @AuthenticationPrincipal MoplUserDetails userDetails
    ) {
        UUID currentUserId = userDetails.getUserDto().id();

        Optional<WatchingSessionDto> sessionDto = watchingSessionService.findCurrentWatchingSessionByUserId(watcherId, currentUserId);

        return ResponseEntity.ok(sessionDto.orElse(null));
    }
}