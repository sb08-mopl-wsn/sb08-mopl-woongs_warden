package com.mopl.mopl.global.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        log.error("로그인 실패, 원인: {}", exception.getClass().getSimpleName());

        String errorMessage;
        String errorCode;
        int status;

        if (exception instanceof BadCredentialsException) {
            errorMessage = "ID/PW가 올바르지 않습니다.";
            errorCode = "BAD_CREDENTIALS";
            status = HttpServletResponse.SC_UNAUTHORIZED;

        } else if (exception instanceof LockedException) {
            errorMessage = "잠긴 계정입니다. 관리자에게 문의하세요.";
            errorCode = "ACCOUNT_LOCKED";
            status = HttpServletResponse.SC_FORBIDDEN;

        } else if (exception instanceof DisabledException) {
            errorMessage = "비활성 계정입니다.";
            errorCode = "ACCOUNT_DISABLED";
            status = HttpServletResponse.SC_FORBIDDEN;

        } else {
            errorMessage = "로그인 실패입니다.";
            errorCode = "AUTHENTICATION_FAILED";
            status = HttpServletResponse.SC_UNAUTHORIZED;
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", errorMessage);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        String responseBody = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(responseBody);
    }
}