package com.mopl.mopl.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 토큰 검증 및 claims 추출 유틸리티
    private final JwtTokenProvider tokenProvider;
    // 사용자 로딩 서비스
    private final MoplUserDetailsService userDetailsService;
    // 에러 응답 작성용
    private final ObjectMapper objectMapper;
    // 토큰 폐기 여부 확인용 저장소
    private final JwtRegistry jwtRegistry;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if ("/api/auth/refresh".equals(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Authorization 헤더에서 Bearer 토큰을 추출한다.
            String token = resolveToken(request);

            // 토큰이 존재하는지 확인한다.
            if (StringUtils.hasText(token)) {
                // Access 토큰 유효성 검사(토큰 타입 검증, 만료 시간 검증, 서명 무결성 검증)
                if (tokenProvider.validateAccessToken(token)) {
                    if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
                        authenticationEntryPoint.commence(
                                request,
                                response,
                                new BadCredentialsException("만료된 토큰입니다.")
                        );
                        return;
                    }

                    MoplUserDetails userDetails = tokenProvider.parseAccessToken(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // 토큰 유효성 검사 실패 시 처리(401)
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new BadCredentialsException("유효하지 않은 토큰입니다.")
                    );
                    return;
                }
            }
        } catch (Exception e) {
            // 인증 과정에서 예외 발생 시 인증 컨텍스트를 초기화하고 401 응답을 반환한다.
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("인증 처리 중 오류가 발생했습니다.")
            );
            return;
        }

        // JWT 기반 인증 후 다음 필터로 체인을 이어간다.
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 헤더에 없으면 쿠키를 뒤져봅니다.
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}