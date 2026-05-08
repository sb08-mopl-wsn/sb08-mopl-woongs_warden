package com.mopl.mopl.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.global.auth.JwtAuthenticationFilter;
import com.mopl.mopl.global.auth.JwtTokenProvider;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.auth.details.MoplUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider tokenProvider;

    @Mock
    MoplUserDetailsService userDetailsService;

    @Mock
    JwtRegistry jwtRegistry;

    @Mock
    FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("/api/auth/refresh 요청은 JWT 필터 인증 로직을 건너뛴다")
    void refreshRequest_skipsJwtAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenProvider,
                userDetailsService,
                new ObjectMapper(),
                jwtRegistry
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenProvider, never()).validateAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("유효한 Access Token이면 SecurityContext에 인증 객체를 저장한다")
    void validAccessToken_setsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenProvider,
                userDetailsService,
                new ObjectMapper(),
                jwtRegistry
        );
        String accessToken = "valid-access-token";
        String email = "admin@admin.com";
        UserDto userDto = new UserDto(UUID.randomUUID(), null, email, "관리자", null, Role.ADMIN, false);
        MoplUserDetails userDetails = new MoplUserDetails(userDto, "encoded-password");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateAccessToken(accessToken)).thenReturn(true);
        when(jwtRegistry.hasActiveJwtInformationByAccessToken(accessToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(accessToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(email);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("검증 실패한 Access Token이면 401을 반환하고 다음 필터로 넘기지 않는다")
    void invalidAccessToken_returnsUnauthorized() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenProvider,
                userDetailsService,
                new ObjectMapper(),
                jwtRegistry
        );
        String accessToken = "invalid-access-token";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateAccessToken(accessToken)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
    }
}
