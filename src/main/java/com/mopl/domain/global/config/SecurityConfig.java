package com.mopl.domain.global.config;

import com.mopl.domain.jwt.JwtAuthenticationFilter;
import com.mopl.domain.jwt.handler.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
//@EnableMethodSecurity  // 기능을 못하게 주석
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtLoginSuccessHandler jwtLoginSuccessHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtLogoutHandler jwtLogoutHandler,
            LoginFailureHandler loginFailureHandler,
            DaoAuthenticationProvider authenticationProvider,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // 문서 관련
//                        .requestMatchers("/", "/index.html").permitAll()
//                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
//
//                        // 로그인/아웃 관련
//                        .requestMatchers("/api/auth/csrf-token").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
//                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll() // 엑세스 토큰 재발급
//
//                        // 채널 관련
//                        .requestMatchers("/api/channels/public").hasRole("CHANNEL_MANAGER")
//                        .requestMatchers(HttpMethod.PATCH, "/api/channels/{channelId}").hasRole("CHANNEL_MANAGER")
//                        .requestMatchers(HttpMethod.DELETE, "/api/channels").hasRole("CHANNEL_MANAGER")
//
//                        // 유저 관련
//                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
//
//                        // 사용자 권한 변경은 ADMIN 권한 필요
//                        .requestMatchers(HttpMethod.PUT, "/api/auth/role").hasRole("ADMIN")
//                        .requestMatchers("/api/**").authenticated()
//                        .anyRequest().authenticated()

                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(jwtLoginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(jwtLogoutHandler)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_CHANNEL_MANAGER > ROLE_USER");
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/favicon.ico", "/error", "/assets/**")
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/*.js", "/*.css");
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}