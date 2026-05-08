package com.mopl.mopl.domain.auth.controller;

import com.mopl.mopl.domain.auth.service.AuthService;
import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MoplUserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        log.debug("CSRF 토큰 요청: {}", csrfToken.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDTO> refresh(
            @CookieValue(
                    name = JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            jwtTokenProvider.expireRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // todo
        String username = jwtTokenProvider.getUserEmailFromToken(refreshToken);
        log.info("refresh userEmail = [{}]", username);

        MoplUserDetails userDetails = (MoplUserDetails) userDetailsService.loadUserByUsername(username);

        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            UserDto userDto = userDetails.getUserDto();

            JwtInformation newJwtInfo = new JwtInformation(userDto, newAccessToken, newRefreshToken);
            jwtRegistry.rotateJwtInformation(refreshToken, newJwtInfo);

            jwtTokenProvider.addRefreshCookie(response, newRefreshToken);

            JwtDTO body = new JwtDTO(userDto, newAccessToken);
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("토큰 재발급 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/role")
    public ResponseEntity<UserDto> updateRole(
            UUID userId,
            @RequestBody UserRoleUpdateRequest userRoleUpdateRequest
    ) {
        UserDto userDto = authService.updateRole(userId,userRoleUpdateRequest);
        return ResponseEntity.ok(userDto);
    }
}