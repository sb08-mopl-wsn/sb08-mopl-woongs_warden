package com.mopl.domain.jwt.dto;

import com.mopl.domain.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JwtInformation {
    private UserDto user;
    private String accessToken;
    private String refreshToken;

    public void rotate(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
