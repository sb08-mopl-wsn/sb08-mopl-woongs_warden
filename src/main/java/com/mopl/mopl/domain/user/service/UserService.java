package com.mopl.mopl.domain.user.service;

import com.mopl.mopl.domain.user.dto.CursorResponseUserDto;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.*;

import java.util.UUID;

public interface UserService {
    UserDto createUser(UserCreateRequest request);
    CursorResponseUserDto getAllUsers(CursorUserRequest request);
    UserDto getUser(UUID userId);
    UserDto updateUserRole(UUID userId,UserRoleUpdateRequest request);
    UserDto updateUserPassword(UUID userId, ChangePasswordRequest request);
    UserDto initUserPassword(UUID userId);
    UserDto updateUserLocked(UUID userId, UserLockUpdateRequest request);
}