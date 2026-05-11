package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.user.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    UserDto getCurrentUserInfo(UserDetails userDetails);
    JwtDTO refresh(String refreshToken, HttpServletResponse response);
}