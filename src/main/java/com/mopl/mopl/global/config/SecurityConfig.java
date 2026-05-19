package com.mopl.mopl.global.config;

import com.mopl.mopl.global.auth.JwtAuthenticationFilter;
import com.mopl.mopl.global.auth.details.GoogleUserDetailsService;
import com.mopl.mopl.global.auth.handler.CustomAccessDeniedHandler;
import com.mopl.mopl.global.auth.handler.CustomAuthenticationEntryPoint;
import com.mopl.mopl.global.auth.handler.JwtLoginSuccessHandler;
import com.mopl.mopl.global.auth.handler.JwtLogoutHandler;
import com.mopl.mopl.global.auth.handler.LoginFailureHandler;
import com.mopl.mopl.global.auth.handler.OAuth2LoginFailureHandler;
import com.mopl.mopl.global.auth.handler.OAuth2LoginSuccessHandler;
import com.mopl.mopl.global.auth.handler.SpaCsrfTokenRequestHandler;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 인증 무시할려면 여기 주석하시면 됩니다.
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* todo 분산환경시 OAuth2 state 저장소를 Redis로 옮기기 등 필요함*/

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtLoginSuccessHandler jwtLoginSuccessHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtLogoutHandler jwtLogoutHandler,
            LoginFailureHandler loginFailureHandler,
            DaoAuthenticationProvider authenticationProvider,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            GoogleUserDetailsService googleUserDetailsService,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oauth2LoginFailureHandler
    ) throws Exception {
        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // 분산환경에서 활성화
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/api/auth/sign-in")
                )
                .authorizeHttpRequests(auth -> auth
                                // 문서 관련
                                .requestMatchers("/", "/index.html").permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()

                                // 로그인/아웃 관련
                                .requestMatchers("/api/auth/sign-in", "/api/auth/sign-out").permitAll()

                                // 유저 관련
                                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/{userId}").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/users/{userId}/password").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/users/*/locked").hasRole("ADMIN")

                                // 인증 관련
                                .requestMatchers(HttpMethod.GET, "/api/auth/csrf-token").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()

                                // 소셜 로그인 관련
                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

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
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(googleUserDetailsService)
                        )
                        .successHandler(oauth2LoginSuccessHandler)
                        .failureHandler(oauth2LoginFailureHandler)
                )
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .addLogoutHandler(jwtLogoutHandler)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
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
                .requestMatchers("/favicon.svg", "/error", "/assets/**")
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

    // 분산 환경용 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CORS 요청을 허용할 주소
        configuration.setAllowedOrigins(List.of(
                // 여기는 예시입니다. 프론트 서버만 연결해보면 될것 같습니다.
                "http://localhost:5173",
                "http://localhost:3000",
                "https://mopl.site"
        ));

        // CORS 요청에서 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        /// 클라이언트 요청에서 허용할 헤더
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "Accept",
                "Last-Event-ID"
        ));

        // 브라우저에서 접근 가능하도록 노출할 응답 헤더
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Location"
        ));

        configuration.setAllowCredentials(true);  // 쿠키, 인증 헤더 등 자격 증명을 포함한 요청 허용
        configuration.setMaxAge(3600L);   // preflight 요청 결과를 브라우저가 캐싱할 시간(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}