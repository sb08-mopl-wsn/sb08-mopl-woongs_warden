package com.mopl.mopl.domain.notification.controller;

import com.mopl.mopl.domain.notification.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final Environment env;

  // 임시 인증 수단 방어 로직
  private void validateTempAuthAllowed() {
    boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
    if (isProd) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "운영 환경에서는 임시 헤더 인증을 사용할 수 없습니다.");
    }
  }

  // 알림 목록 조회
  @GetMapping
  public ResponseEntity<CursorResponseNotificationDto> getNotifications(
      @RequestHeader(value = "X-Temp-User-Id", required = true) UUID userId, // 임시 헤더
      @Valid @ModelAttribute CursorPaginationRequest request
  ) {

    validateTempAuthAllowed();

    CursorResponseNotificationDto response = notificationService.getNotifications(
        userId, request);

    return ResponseEntity.ok(response);
  }

  // 알림 단건 삭제
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> deleteNotification(
      @RequestHeader(value = "X-Temp-User-Id", required = true) UUID userId, // 임시 헤더
      @PathVariable("notificationId") UUID notificationId
  ) {

    validateTempAuthAllowed();

    notificationService.deleteNotification(userId, notificationId);
    return ResponseEntity.noContent().build();
  }
}
