package com.mopl.mopl.global.sse.controller;

import com.mopl.mopl.global.sse.service.SseService;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

  private final SseService sseService;
  private final Environment env;

  // 임시 인증 수단 방어 로직
  private void validateTempAuthAllowed() {
    boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
    if (isProd) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "운영 환경에서는 임시 헤더 인증을 사용할 수 없습니다.");
    }
  }

  // 클라이언트-서버 간 실시간 통신 파이프라인 최초 연결 요청 API
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @RequestHeader(value = "X-Temp-User-Id", required = true) UUID userId // 임시 헤더
  ) {

    validateTempAuthAllowed();

    /* TODO: 인증 객체 병합 후 아래 형태로 변경 요망
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        return sseService.subscribe(userDetails.getId());
    }
    */
    return sseService.subscribe(userId);
  }
}
