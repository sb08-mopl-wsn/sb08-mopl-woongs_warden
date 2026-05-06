package com.mopl.domain.jwt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.jwt.details.moplUserDetails;
import com.mopl.domain.user.dto.UserDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler  implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof moplUserDetails moplUserDetails) {
            UserDto user = moplUserDetails.getUserDto();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            String responseBody = objectMapper.writeValueAsString(user);
            response.getWriter().write(responseBody);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"인증 정보 처리 불가\"}");
        }
    }
}