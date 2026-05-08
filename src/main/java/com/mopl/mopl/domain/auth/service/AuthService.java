package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface AuthService {

    UserDto getCurrentUserInfo(UserDetails userDetails);

    UserDto updateRole(UUID userId, UserRoleUpdateRequest userRoleUpdateRequest);
}
