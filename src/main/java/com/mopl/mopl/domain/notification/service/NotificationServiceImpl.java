package com.mopl.mopl.domain.notification.service;

import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.exception.NotificationNotFoundException;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  /**
   * 특정 알림을 단건 삭제(읽음 처리)하는 메서드
   * @param userId 알림 삭제를 요청한 현재 로그인 사용자의 ID
   * @param notificationId 삭제할 알림의 고유 ID
   */
  @Override
  @Transactional
  public void deleteNotification(UUID userId, UUID notificationId) {

    Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));

    notificationRepository.delete(notification);
  }

  /**
   * 사용자의 알림 목록을 커서 기반 페이지네이션 방식으로 조회 (무한 스크롤)
   * @param userId 알림 목록을 조회할 현재 로그인 사용자의 ID
   * @param cursor 페이지네이션 기준점 (이전 페이지 마지막 알림의 생성 시간)
   * @param idAfter 커서(시간)가 완전히 동일한 데이터가 있을 때 순서 보장을 위한 보조 커서
   * @param limit 한 번에 조회할 알림의 최대 개수
   * @param sortDirection 정렬 방향 (기본값: DESCENDING)
   * @param sortBy 정렬 기준 (createdAt 고정)
   * @return 알림 목록, 다음 커서 정보, 총 개수 등이 포함된 커서 응답 DTO 객체
   */
  @Override
  public CursorResponseNotificationDto getNotifications(
      UUID userId, String cursor, UUID idAfter, int limit, String sortDirection, String sortBy
  ) {

    // 커서 시간 파싱
    Instant cursorTime = (cursor != null && !cursor.isBlank())
        ? Instant.parse(cursor)
        : null;

    // DB 조회 (Limit보다 1개 더 많이 가져와서 다음 페이지 존재여부 확인)
    PageRequest pageRequest = PageRequest.of(0, limit + 1);
    List<Notification> notifications;

    if ("ASCENDING".equalsIgnoreCase(sortDirection)) {
      notifications = notificationRepository.findNotificationsByCursorAsc(userId, cursorTime, idAfter, pageRequest);
    } else {
      notifications = notificationRepository.findNotificationsByCursorDesc(userId, cursorTime, idAfter, pageRequest);
    }

    // 다음 페이지 존재 확인
    boolean hasNext = notifications.size() > limit;
    if (hasNext) {
      notifications.remove(limit);
    }

    // 다음 페이지 조회를 위해서 커서 정보 춫출
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (!notifications.isEmpty()) {
      Notification lastNotification = notifications.get(notifications.size() - 1);
      nextCursor = lastNotification.getCreatedAt().toString();
      nextIdAfter = lastNotification.getId();
    }

    // 사용자 총 알림 개수
    long totalCount = notificationRepository.countByUserId(userId);

    // Dto 변환
    List<NotificationDto> data = notifications.stream()
        .map(notificationMapper::toDto)
        .toList();

    // dto 리턴
    return new CursorResponseNotificationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection
    );
  }
}