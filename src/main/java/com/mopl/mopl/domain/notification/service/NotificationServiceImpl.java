package com.mopl.mopl.domain.notification.service;

import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.exception.InvalidCursorFormatException;
import com.mopl.mopl.domain.notification.exception.InvalidSortParameterException;
import com.mopl.mopl.domain.notification.exception.NotificationNotFoundException;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
   * 특정 알림을 단건 삭제하는 메서드
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
   * @param request 페이지네이션 요청 파라미터 (cursor, limit, 정렬 정보 등)
   * @return 알림 목록, 다음 커서 정보, 총 개수 등이 포함된 커서 응답 DTO 객체
   */
  @Override
  public CursorResponseNotificationDto getNotifications(UUID userId, CursorPaginationRequest request) {

    // 파라미터가 비어있으면 기본값 createdAt으로 덮어씌움
    String sortBy = (request.sortBy() == null || request.sortBy().isBlank()) ? "createdAt" : request.sortBy();

    // 정렬 파라미터 검증 (WhiteList 검사)
    if (!"createdAt".equals(sortBy)) {
      throw new InvalidSortParameterException("정렬 기준(sortBy)은 'createdAt'만 지원합니다.");
    }

    if (!"ASCENDING".equalsIgnoreCase(request.sortDirection()) && !"DESCENDING".equalsIgnoreCase(request.sortDirection())) {
      throw new InvalidSortParameterException("정렬 방향(sortDirection)은 'ASCENDING' 또는 'DESCENDING'만 지원합니다.");
    }

    // 커서 시간 파싱, 예외처리 방어로직
    Instant cursorTime = null;
    if (request.cursor() != null && !request.cursor().isBlank()) {
      try {
        cursorTime = Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new InvalidCursorFormatException();
      }
    }

    // DB 조회 (Limit보다 1개 더 많이 가져와서 다음 페이지 존재여부 확인)
    PageRequest pageRequest = PageRequest.of(0, request.limit() + 1);
    List<Notification> notifications;

    if ("ASCENDING".equalsIgnoreCase(request.sortDirection())) {
      notifications = notificationRepository.findNotificationsByCursorAsc(userId, cursorTime, request.idAfter(), pageRequest);
    } else {
      notifications = notificationRepository.findNotificationsByCursorDesc(userId, cursorTime, request.idAfter(), pageRequest);
    }

    // 다음 페이지 존재 확인
    boolean hasNext = notifications.size() > request.limit();
    if (hasNext) {
      notifications.remove(request.limit().intValue());
    }

    // 다음 페이지 조회를 위해서 커서 정보 추출
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (!notifications.isEmpty()) {
      Notification lastNotification = notifications.get(notifications.size() - 1);
      nextCursor = lastNotification.getCreatedAt().toString();
      nextIdAfter = lastNotification.getId();
    }

    // 사용자 총 알림 개수 (첫 페이지에서만 count 쿼리 실행)
    long totalCount = -1L;
    if (request.cursor() == null || request.cursor().isBlank()){
      totalCount = notificationRepository.countByUserId(userId);
    }

    // Dto 변환
    List<NotificationDto> data = notifications.stream()
        .map(notificationMapper::toDto)
        .toList();

    // dto 리턴
    return new CursorResponseNotificationDto(
        data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, request.sortDirection()
    );
  }
}