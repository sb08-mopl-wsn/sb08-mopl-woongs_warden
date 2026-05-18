package com.mopl.mopl.global.auth.handler;

import com.mopl.mopl.domain.jwt.dto.JwtInformation;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.exception.oauth2.OAuth2PrincipalException;
import com.mopl.mopl.global.exception.oauth2.Oauth2FailedTokenException;
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

        if (!(authentication.getPrincipal() instanceof MoplUserDetails userDetails)) {
            throw new OAuth2PrincipalException();
        }

        try {
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
            response.sendRedirect(successRedirectUrl);

        } catch (Exception e) {
            throw new Oauth2FailedTokenException();
        }
    }
}