package com.mopl.mopl.global.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.auth.service.AuthenticationAttemptService;
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
    private final AuthenticationAttemptService authenticationAttemptService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.error("로그인 실패, 원인: {}", exception.getClass().getSimpleName());
        String username = request.getParameter("username");
        if (username == null || username.isBlank()) {
            username = request.getParameter("email");
        }

        boolean loginAttemptLocked = exception instanceof BadCredentialsException
                && authenticationAttemptService.recordLoginFailure(username);

        String errorMessage;
        String errorCode;
        int status;

        if (exception instanceof BadCredentialsException) {
            errorMessage = loginAttemptLocked
                    ? "비밀번호를 5회 틀려 30분간 로그인이 제한됩니다."
                    : "ID/PW가 올바르지 않습니다.";

            errorCode = loginAttemptLocked ? "LOGIN_LOCKED" : "AUTHENTICATION_FAILED";
            status = loginAttemptLocked ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED;

        } else if (exception instanceof LockedException) {
            errorMessage = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "잠긴 계정입니다. 관리자에게 문의하세요."
                    : exception.getMessage();

            errorCode = "ACCOUNT_LOCKED";
            status = HttpServletResponse.SC_FORBIDDEN;

        } else if (exception instanceof DisabledException) {
            errorMessage = "비활성 계정입니다.";
            errorCode = "AUTHENTICATION_FAILED";
            status = HttpServletResponse.SC_FORBIDDEN;

        } else {
            errorMessage = "로그인 실패 입니다.";
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