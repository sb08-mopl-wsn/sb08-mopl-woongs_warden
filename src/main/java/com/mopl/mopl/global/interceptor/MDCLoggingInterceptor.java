package com.mopl.mopl.global.interceptor;

import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class MDCLoggingInterceptor implements HandlerInterceptor {

  public static final String REQUEST_ID = "requestId";
  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_METHOD = "requestMethod";
  public static final String REQUEST_URI = "requestUri";
  public static final String USER_ID = "userId";

  @Override
  public boolean preHandle(
          HttpServletRequest request,
          HttpServletResponse response,
          Object handler
  ) {
    String requestId = UUID.randomUUID().toString().replaceAll("-", "");
    String clientIp = getClientIp(request);
    String userId = resolveUserId();

    MDC.put(REQUEST_ID, requestId);
    MDC.put(REQUEST_METHOD, request.getMethod());
    MDC.put(REQUEST_URI, request.getRequestURI());
    MDC.put("client-ip", clientIp);

    if (userId != null) {
      MDC.put(USER_ID, userId);
    }

    response.setHeader(REQUEST_ID_HEADER, requestId);
    response.setHeader("X-Client-IP", clientIp);

    return true;
  }

  private String resolveUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof MoplUserDetails userDetails) {
      return userDetails.getUserDto().id().toString();
    }

    return null;
  }

  private String getClientIp(HttpServletRequest request) {
    String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    for (String header : headerNames) {
      String ip = request.getHeader(header);

      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        if (ip.contains(",")) {
          return ip.split(",")[0].trim();
        }
        return ip;
      }
    }

    return request.getRemoteAddr();
  }

  @Override
  public void afterCompletion(
          HttpServletRequest request,
          HttpServletResponse response,
          Object handler,
          Exception ex
  ) {
    MDC.clear();
  }
}