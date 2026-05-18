package com.mopl.mopl.global.auth.handler;

import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Value("${app.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            MoplUserDetails userDetails = (MoplUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            UserDto userDto = userDetails.getUserDto();

            JwtInformation jwtInformation = new JwtInformation(
                    userDto,
                    accessToken,
                    refreshToken
            );

            jwtRegistry.registerJwtInformation(jwtInformation);
            jwtTokenProvider.addRefreshCookie(response, refreshToken);

            /*
             * 권장 방식:
             * accessToken을 URL에 직접 싣지 않고,
             * refreshToken 쿠키만 설정한 뒤 프론트로 이동합니다.
             *
             * 프론트는 /oauth2/redirect 진입 후
             * POST /api/auth/refresh 를 호출해서 accessToken을 받으면 됩니다.
             */
            response.sendRedirect(successRedirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "OAuth2 token generation failed"
            );
        }
    }
}