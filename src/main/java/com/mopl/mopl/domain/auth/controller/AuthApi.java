package com.mopl.mopl.domain.auth.controller;

import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApi {

    @Operation(summary = "CSRF 토큰 발급")
    @ApiResponse(responseCode = "204", description = "발급 성공")
    ResponseEntity<Void> getCsrfToken(
            @Parameter(hidden = true) CsrfToken csrfToken
    );

    @Operation(summary = "토큰 재발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "500", description = "재발급 실패")
    })
    ResponseEntity<JwtDTO> refresh(
            @Parameter(hidden = true)
            @CookieValue(
                    name = JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken,

            @Parameter(hidden = true)
            HttpServletResponse response
    );

    @Operation(summary = "비밀번호 초기화")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기화 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    );
}