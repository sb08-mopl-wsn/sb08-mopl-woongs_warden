package com.mopl.mopl.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler
  ) throws Exception {
    // todo 여기도 수정해야함
    String userIdHeader = request.getHeader("X-User-Id");

    // 헤더 누락 확인
    if (userIdHeader == null || userIdHeader.isEmpty()) {
      // TODO 여기 예러처리 수정 필요
      throw new MissingRequestHeaderException("X-User-Id", null);
//      throw new AuthenticationException();
    }

    try {
      // UUID 패턴인지 확인
      // String → UUID 변환할 때, String이 UUID 형식이 아니면 IllegalArgumentException 에러 발생하기 때문에 try문 사용
      UUID userId = UUID.fromString(userIdHeader);

      // 컨트롤러에서 쉽게 추출하기 위함
      request.setAttribute("requestUserId", userId);

      return true;
    } catch (IllegalArgumentException e) {
      throw new MissingRequestHeaderException("X-User-Id", null);
//      throw new AuthenticationException();
    }
  }
}
