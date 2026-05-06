package com.mopl.domain.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 토큰 검증 및 claims 추출 유틸리티
    private final JwtTokenProvider tokenProvider;
    // 사용자 로딩 서비스
//    private final moplUserDetailsService userDetailsService;
//    // 에러 응답 작성용
//    private final ObjectMapper objectMapper;
//    // 토큰 폐기 여부 확인용 저장소
//    private final JwtRegistry jwtRegistry;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 일단 포스트맨도 동작하게
        filterChain.doFilter(request, response);
        return;

//        String requestURI = request.getRequestURI();
//        if ("/api/auth/refresh".equals(requestURI)) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        try {
//            // Authorization 헤더에서 Bearer 토큰을 추출한다.
//            String token = resolveToken(request);
//
//            // 토큰이 존재하는지 확인한다.
//            if (StringUtils.hasText(token)) {
//                // Access 토큰 유효성 검사(토큰 타입 검증, 만료 시간 검증, 서명 무결성 검증)
//                if (tokenProvider.validateAccessToken(token)) {
//                    if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
//                        sendUnauthorized(response, "Token revoked");
//                        return;
//                    }
//
//                    String username = tokenProvider.getUsernameFromToken(token);
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(
//                                    userDetails,
//                                    null,
//                                    userDetails.getAuthorities()
//                            );
//
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                } else {
//                    // 토큰 유효성 검사 실패 시 처리(401)
//                    sendUnauthorized(response, "Invalid JWT token");
//                    return;
//                }
//            }
//        } catch (Exception e) {
//            // 인증 과정에서 예외 발생 시 인증 컨텍스트를 초기화하고 401 응답을 반환한다.
//            SecurityContextHolder.clearContext();
//            sendUnauthorized(response, "JWT authentication failed");
//            return;
//        }
//
//        // JWT 기반 인증 후 다음 필터로 체인을 이어간다.
//        filterChain.doFilter(request, response);
//    }
//
//    private String resolveToken(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//
//        // 헤더에 없으면 쿠키를 뒤져봅니다.
//        if (request.getCookies() != null) {
//            return Arrays.stream(request.getCookies())
//                    .filter(cookie -> "accessToken".equals(cookie.getName()))
//                    .map(Cookie::getValue)
//                    .findFirst()
//                    .orElse(null);
//        }
//        return null;
    }

//    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
//        // 응답 헤더 설정
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        response.setCharacterEncoding("UTF-8");
//
//        // JSON 응답 전송
//        String responseBody = objectMapper.createObjectNode()
//                .put("success", false)
//                .put("message", message)
//                .toString();
//
//        // 응답 바디 전송
//        response.getWriter().write(responseBody);
//    }
}