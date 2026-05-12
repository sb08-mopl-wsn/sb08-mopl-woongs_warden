package com.mopl.mopl.global.auth;

import com.mopl.mopl.domain.jwt.entity.Jwt;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    private final int accessTokenExpirationMs;
    private final int refreshTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier refreshTokenVerifier;

    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public JwtTokenProvider(
            @Value("${jwt.access-token.secret}") String accessTokenSecret,
            @Value("${jwt.access-token.exp}") int accessTokenExpirationMs,
            @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
            @Value("${jwt.refresh-token.exp}") int refreshTokenExpirationMs,
            @Value("${jwt.refresh-token.cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${jwt.refresh-token.cookie.same-site:Lax}") String refreshCookieSameSite
    ) throws JOSEException {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

        byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
    }

    public String generateAccessToken(MoplUserDetails userDetails) throws JOSEException {
        return generateToken(userDetails, accessTokenExpirationMs, accessTokenSigner, "access");
    }

    public String generateRefreshToken(MoplUserDetails userDetails) throws JOSEException {
        return generateToken(userDetails, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
    }

    private String generateToken(
            MoplUserDetails userDetails,
            int expirationMs, JWSSigner signer,
            String tokenType
    ) throws JOSEException {

        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // 토큰의 클레임(claims)을 설정
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                // 토큰 주체(sub)
                .subject(userDetails.getUsername())
                // 토큰 고유 식별자(jti)
                .jwtID(tokenId)
                // 사용자 ID(userId)
                .claim("userId", userDetails.getUserDto().id())
                .claim("userEmail", userDetails.getUserDto().email())
                .claim("name", userDetails.getUserDto().name())
                // 토큰 타입(type)
                .claim("type", tokenType)
                // 사용자 권한(roles)
                .claim("roles",
                        userDetails.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
                // 토큰 발급 시간(iat)
                .issueTime(now)
                // 토큰 만료 시간(exp)
                .expirationTime(expiryDate)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        String completedJWT = signedJWT.serialize();
        return completedJWT;
    }

    public ResponseCookie generateRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(refreshTokenExpirationMs / 1000)
                .sameSite(refreshCookieSameSite)
                .build();
    }

    public ResponseCookie generateRefreshTokenExpirationCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(refreshCookieSameSite)
                .build();
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = generateRefreshTokenCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void expireRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = generateRefreshTokenExpirationCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public boolean validateAccessToken(String token) {
        boolean result = verifyToken(token, accessTokenVerifier, "access");
        return result;
    }

    public boolean validateRefreshToken(String token) {
        boolean result = verifyToken(token, refreshTokenVerifier, "refresh");
        return result;
    }

    private boolean verifyToken(String token, JWSVerifier verifier, String expectedType) {
        try {

            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(verifier)) {
                return false;
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                return false;
            }

            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean valid = exp != null && exp.after(new Date());
            return valid;

        } catch (Exception e) {
            return false;
        }
    }

    public String getUserEmailFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return subject;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public String getTokenId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            return jti;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Date getExpiration(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            return exp;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Object userId = signedJWT.getJWTClaimsSet().getClaim("userId");

            if (userId == null) {
                throw new IllegalArgumentException("userId claim not found");
            }

            return UUID.fromString(userId.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    // JwtToken Claim에서 userId, username, role을 직접 파싱하여 DB조회 없이 가져오기 위함.
    public MoplUserDetails parseAccessToken(String token) {
        try {
            log.info("[TokenProvider] parseAccessToken 호출됨: 토큰 파싱 시작");
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            String username = claimsSet.getSubject();
            String userEmail = claimsSet.getStringClaim("userEmail");
            String name = claimsSet.getStringClaim("name");
            String userIdString = claimsSet.getStringClaim("userId");
            UUID userId = userIdString != null ? UUID.fromString(userIdString) : null;

            List<String> roles = claimsSet.getStringListClaim("roles");
            // 기본 값
            Role role = Role.USER;

            if (roles != null && !roles.isEmpty()) {
                // TODO: ADMIN, USER 제외 예외처리 필
                String roleString = roles.get(0).replace("ROLE_", "");
                role = Role.valueOf(roleString);
            }

            UserDto userDto = new UserDto(
                    userId,
                    null,
                    userEmail, // email
                    name,
                    null, // profile
                    role,
                    false
            );

            log.info("[TokenProvider] parseAccessToken 완료: UserDto 생성");
            return new MoplUserDetails(userDto, null);

        } catch (Exception e) {
            log.error("[TokenProvider] parseAccessToken 중 예외 발생: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}