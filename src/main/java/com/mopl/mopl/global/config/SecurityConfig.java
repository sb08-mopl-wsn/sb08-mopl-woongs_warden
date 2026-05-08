package com.mopl.mopl.global.config;

import com.mopl.mopl.global.auth.JwtAuthenticationFilter;
import com.mopl.mopl.global.auth.handler.*;
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
@EnableMethodSecurity  // 인증 무시할려면 여기 주석하시면 됩니다.
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
                        .ignoringRequestMatchers(
                                "/api/auth/sign-in"
//                                "/api/auth/sign-out",
//                                "/api/auth/refresh"
                        )
                )
                .authorizeHttpRequests(auth -> auth
                                // 문서 관련
                                .requestMatchers("/", "/index.html").permitAll()
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()

                                // 로그인/아웃 관련
                                .requestMatchers("/api/auth/csrf-token").permitAll()
                                .requestMatchers("/api/auth/sign-in", "/api/auth/sign-out").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll() // 엑세스 토큰 재발급

                                // 유저 관련
                                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                                // 사용자 권한 변경은 ADMIN 권한 필요
                                .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/users/*/locked").hasRole("ADMIN")
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().authenticated()

                        // 인증 무시할려면 여기 주석 해제하고
                        // 위에 체인메서드 전부 주석하시면 됩니다.
                        // .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/sign-in")
                        .successHandler(jwtLoginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
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
        RoleHierarchy hierarchy = RoleHierarchyImpl.fromHierarchy("ADMIN > USER");
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

    // todo 여기 지금 경고 나온다고 함
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