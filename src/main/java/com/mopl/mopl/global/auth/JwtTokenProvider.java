package com.mopl.mopl.global.auth;

import com.mopl.mopl.domain.jwt.entity.Jwt;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH-TOKEN";
    private final int accessTokenExpirationMs;
    private final int refreshTokenExpirationMs;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier refreshTokenVerifier;
    private final UserRepository userRepository;  //todo 이거 관해서 수정해야할거 같은데

    public JwtTokenProvider(
            // application.yaml 파일에 정의된 프로퍼티 값을 주입받는다.
            @Value("${jwt.access-token.secret}") String accessTokenSecret,
            @Value("${jwt.access-token.exp}") int accessTokenExpirationMs,
            @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
            @Value("${jwt.refresh-token.exp}") int refreshTokenExpirationMs,
            UserRepository userRepository
    ) throws JOSEException {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        byte[] accessSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

        byte[] refreshSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);

        this.userRepository = userRepository;
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

    public Cookie generateRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenExpirationMs / 1000);
        return cookie;
    }

    public Cookie generateRefreshTokenExpirationCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = generateRefreshTokenCookie(refreshToken);
        response.addCookie(cookie);
    }

    public void expireRefreshCookie(HttpServletResponse response) {
        Cookie cookie = generateRefreshTokenExpirationCookie();
        response.addCookie(cookie);
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

    public String getUsernameFromToken(String token) {
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

    public Date getIssuedAt(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date iat = signedJWT.getJWTClaimsSet().getIssueTime();
            return iat;

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

    public Jwt toEntity(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String username = signedJWT.getJWTClaimsSet().getSubject();

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            Instant issuedAt = signedJWT.getJWTClaimsSet().getIssueTime().toInstant();
            Instant expiresAt = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();

            // todo 여기도 수정 필요
            User user = userRepository.findByName(username)
                    .orElseThrow(()->new IllegalArgumentException("User not found"));

            //todo 여기 token이 리프레쉬였는지 확인 필요
            Jwt entity = new Jwt(user, issuedAt,token );
            return entity;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}