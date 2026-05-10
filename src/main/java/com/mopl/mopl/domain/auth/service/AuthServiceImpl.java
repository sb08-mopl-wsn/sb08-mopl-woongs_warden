package com.mopl.mopl.domain.auth.service;

import com.mopl.mopl.domain.auth.exception.AuthExpiredTokenException;
import com.mopl.mopl.domain.auth.exception.AuthInvalidTokenException;
import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final JwtRegistry jwtRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    private final MoplUserDetailsService userDetailsService;

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserInfo(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        String username = userDetails.getUsername();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public JwtDTO refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AuthInvalidTokenException();
        }

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            jwtTokenProvider.expireRefreshCookie(response);
            throw new AuthExpiredTokenException();
        }

        String userEmail = jwtTokenProvider.getUserEmailFromToken(refreshToken);
        JwtInformation oldJwtInfo = jwtRegistry.getJwtInformationByRefreshToken(refreshToken);


        MoplUserDetails userDetails =
                (MoplUserDetails) userDetailsService.loadUserByUsername(userEmail);

        String newRefreshToken = null;

        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            UserDto userDto = userDetails.getUserDto();

            JwtInformation newJwtInfo = new JwtInformation(
                    userDto,
                    newAccessToken,
                    newRefreshToken
            );

            jwtRegistry.rotateJwtInformation(refreshToken, newJwtInfo);
            jwtTokenProvider.addRefreshCookie(response, newRefreshToken);

            return new JwtDTO(userDto, newAccessToken);

        } catch (Exception e) {
            jwtRegistry.rollbackRotateJwtInformation(
                    refreshToken,
                    oldJwtInfo,
                    newRefreshToken
            );

            log.error("토큰 재발급 중 오류 발생", e);
            throw new RuntimeException("토큰 재발급 중 오류가 발생했습니다.", e);
        }
    }
}