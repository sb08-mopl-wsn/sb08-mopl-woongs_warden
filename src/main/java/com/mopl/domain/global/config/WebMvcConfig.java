package com.mopl.domain.global.config;

import com.mopl.domain.global.interceptor.AuthenticationInterceptor;
import com.mopl.domain.global.interceptor.MDCLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final MDCLoggingInterceptor mdcLoggingInterceptor;
  private final AuthenticationInterceptor authenticationInterceptor;

  @Value("${auth.interceptor.enabled:true}")
  private boolean authInterceptorEnabled;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // MDC 로그 인터셉터
    registry.addInterceptor(mdcLoggingInterceptor)
        .addPathPatterns("/**")
        .order(1);

    // 기본으로 실행되지만 테스트에서만 안되게 막음
    // application-test.yaml의 auth.interceptor.enabled 확인
    if (authInterceptorEnabled) {
      // 인증 헤더확인 인터셉터
      registry
          .addInterceptor(authenticationInterceptor)
          .addPathPatterns("/**")
          .excludePathPatterns(
              // 인증 제외 대상
              "/",
              "/index.html",
              "/api/users",
              "/api/users/login",
              "/swagger-ui/**",
              "/v3/api-docs/**",
              "/assets/**",
              "/images/**",
              "/favicon.ico"
          )
          .order(2);
    }
  }
}
