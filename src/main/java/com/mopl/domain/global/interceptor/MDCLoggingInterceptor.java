package com.mopl.domain.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class MDCLoggingInterceptor implements HandlerInterceptor {

  public static final String REQUEST_ID = "requestId";
  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_METHOD = "requestMethod";
  public static final String REQUEST_URI = "requestUri";
  // todo 여기도 수정해야함
//  public static final String DEOKHUGAM_USER_ID = "Deokhugam-Request-User-ID";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String requestId = UUID.randomUUID().toString().replaceAll("-", "");
//    String deokhugamUserId = request.getHeader(DEOKHUGAM_USER_ID);
    String clientIp = getClientIp(request);

    MDC.put(REQUEST_ID, requestId);
    MDC.put(REQUEST_METHOD, request.getMethod());
    MDC.put(REQUEST_URI, request.getRequestURI());
    MDC.put("client-ip", clientIp);

    // 요청헤더에 있을 때만 추가
//    if (deokhugamUserId != null) {
//      MDC.put(DEOKHUGAM_USER_ID, deokhugamUserId);
//      response.setHeader(DEOKHUGAM_USER_ID, deokhugamUserId);
//    }

    response.setHeader(REQUEST_ID_HEADER, requestId);
    response.setHeader("X-Client-IP", clientIp);

    return true;
  }

  // 실제 사용자 IP를 정확히 가져오기 위한 메서드
  private String getClientIp(HttpServletRequest request) {
    // AWS 등 프록시 환경에서는 서버의 request.getRemoteAddr()이 로드밸런서 IP를 반환함.
    // 실제 클라이언트의 IP를 식별하기 위해 X-Forwarded-For 등 프록시 헤더를 순차적으로 확인.
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

        // AWS에서 로드 벨런스 같은 것을 거치면 IP가 ,(콤마)를 기준으로 여러개가 된다고 함
        // e.g. X-Forwarded-For: <Client_IP>, <Proxy1_IP>, <Proxy2_IP>
        // 맨 처음이 사용자의 원본IP이므로 첫 번째만 추출한다.
        if(ip.contains(",")) {
          return ip.split(",")[0];
        }
        return ip;
      }
    }

    return request.getRemoteAddr();
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    MDC.clear();
  }
}
