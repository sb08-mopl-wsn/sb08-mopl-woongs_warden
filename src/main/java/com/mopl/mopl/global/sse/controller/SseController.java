package com.mopl.mopl.global.sse.controller;

import com.mopl.mopl.global.auth.details.MoplUserDetails;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController implements SseApi {

  private final SseService sseService;

  // 클라이언트-서버 간 실시간 통신 파이프라인 최초 연결 요청 API
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails
  ) {
    return sseService.subscribe(userDetails.getUserDto().id());
  }
}
