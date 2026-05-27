package com.mopl.mopl.global.auth.handler;

import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {
    private final JwtTokenProvider tokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        UUID userId = null;

        // 1. 정상적으로 인증된 상태라면 Authentication 객체에서 바로 추출
        if (authentication != null && authentication.getPrincipal() instanceof MoplUserDetails userDetails) {
            userId = userDetails.getUserDto().id();
        }

        // 2. Access Token이 만료되어 Authentication이 없는 경우, 헤더에서 토큰을 추출해 강제로 파싱
        if (userId == null) {
            String authz = request.getHeader("Authorization");
            if (authz != null && authz.startsWith("Bearer ")) {
                try {
                    String accessToken = authz.substring(7);
                    userId = extractUserIdFromToken(accessToken);
                } catch (Exception ignored) {
                }
            }
        }

        // 3. 헤더에도 없다면, 리프레시 토큰 쿠키에서 추출 시도
        if (userId == null && request.getCookies() != null) {
            Optional<Cookie> rtCookie = Arrays.stream(request.getCookies())
                    .filter(c -> JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                    .findFirst();
            if (rtCookie.isPresent()) {
                try {
                    userId = extractUserIdFromToken(rtCookie.get().getValue());
                } catch (Exception ignored) {
                }
            }
        }

        // 4. 사용자 ID를 찾았다면 Registry에서 해당 유저의 모든 토큰 무효화
        if (userId != null) {
            jwtRegistry.invalidateJwtInformationByUserId(userId);
            log.debug("[Logout] 유저 ID: {} 의 모든 JWT 세션이 무효화되었습니다.", userId);
        } else {
            log.warn("[Logout] 로그아웃 요청에서 유저 식별 정보를 찾을 수 없습니다.");
        }

        // 5. 클라이언트의 리프레시 쿠키를 빈 값(MaxAge=0)으로 덮어씌워 브라우저에서 삭제 유도
        tokenProvider.expireRefreshCookie(response);
    }

    private UUID extractUserIdFromToken(String token) throws Exception {
        if(!tokenProvider.validateRefreshToken(token)) {
            if (tokenProvider.validateAccessToken(token) || tokenProvider.validateRefreshToken(token)) {
                return tokenProvider.getUserIdFromToken(token);
            }
            return null;
        }

        SignedJWT signedJWT = SignedJWT.parse(token);
        Object claim = signedJWT.getJWTClaimsSet().getClaim("userId");
        if (claim != null) {
            return UUID.fromString(claim.toString());
        }
        return null;
    }
}