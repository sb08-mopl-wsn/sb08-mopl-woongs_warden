package com.mopl.mopl.global.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.dto.JwtDTO;
import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider tokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // 응답 인코딩/콘텐츠 타입 설정
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Principal 유효성 확인 및 캐스팅
        if (authentication.getPrincipal() instanceof MoplUserDetails customUserDetails) {
            try {// 사용자 DTO 구성
                UserDto userDto = customUserDetails.getUserDto();

                // 1. 새 Access/Refresh 토큰 발급
                String accessToken = tokenProvider.generateAccessToken(customUserDetails);
                String refreshToken = tokenProvider.generateRefreshToken(customUserDetails);

                // 2. JwtRegistry에 등록
                // (이 메서드 내부에서 기존 토큰 무효화, 동시 로그인 제한, 새 토큰 DB/메모리 저장이 모두 자동으로 처리됩니다)
                JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken);
                jwtRegistry.registerJwtInformation(jwtInformation);

                // 3. 리프레시 쿠키 설정
                tokenProvider.addRefreshCookie(response, refreshToken);

                // 4. JwtDto 바디 전송 (Access Token 응답)
                JwtDTO jwtDto = new JwtDTO(userDto, accessToken);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(objectMapper.writeValueAsString(jwtDto));

            } catch (Exception e) {
                // 예외 발생 시 처리(500)
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(objectMapper.createObjectNode()
                        .put("success", false)
                        .put("message", "Token generation failed")
                        .toString());
            }
        } else {
            // 인증 실패 시 처리(401)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(objectMapper.createObjectNode()
                    .put("success", false)
                    .put("message", "Invalid principal")
                    .toString());
        }
    }
}