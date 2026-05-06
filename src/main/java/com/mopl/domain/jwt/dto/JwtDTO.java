package com.mopl.domain.jwt.dto;

import com.mopl.domain.user.dto.UserDto;

public record JwtDTO(UserDto userDto, String accessToken) {}