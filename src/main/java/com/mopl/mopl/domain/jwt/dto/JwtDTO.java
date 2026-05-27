package com.mopl.mopl.domain.jwt.dto;

import com.mopl.mopl.domain.user.dto.UserDto;

public record JwtDTO(UserDto userDto, String accessToken) {}