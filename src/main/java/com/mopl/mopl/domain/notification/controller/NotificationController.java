package com.mopl.mopl.domain.notification.controller;

import com.mopl.mopl.domain.notification.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.service.NotificationService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  // 알림 목록 조회
  @GetMapping
  public ResponseEntity<CursorResponseNotificationDto> getNotifications(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @Valid @ModelAttribute CursorPaginationRequest request
  ) {

    UUID userId = userDetails.getUserDto().id();
    CursorResponseNotificationDto response = notificationService.getNotifications(
        userId, request);

    return ResponseEntity.ok(response);
  }

  // 알림 단건 삭제
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> deleteNotification(
      @AuthenticationPrincipal(errorOnInvalidType = true) MoplUserDetails userDetails,
      @PathVariable("notificationId") UUID notificationId
  ) {

    UUID userId = userDetails.getUserDto().id();
    notificationService.deleteNotification(userId, notificationId);
    return ResponseEntity.noContent().build();
  }
}
