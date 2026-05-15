package com.mopl.mopl.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.exception.InvalidCursorFormatException;
import com.mopl.mopl.domain.notification.exception.NotificationNotFoundException;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @InjectMocks
  private NotificationServiceImpl notificationService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationMapper notificationMapper;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.builder().email("test@test.com").build();
    ReflectionTestUtils.setField(user, "id", userId);
  }

  @Test
  @DisplayName("알림 삭제 - 정상적으로 삭제 완료")
  void deleteNotification_Success() {

    // given
    UUID notificationId = UUID.randomUUID();
    Notification notification = Notification.builder()
        .user(user)
        .title("테스트 알림")
        .content("테스트 내용")
        .level(NotificationLevel.INFO)
        .build();
    ReflectionTestUtils.setField(notification, "id", notificationId);

    given(notificationRepository.findByIdAndUserId(notificationId, userId))
        .willReturn(Optional.of(notification));

    // when
    notificationService.deleteNotification(userId, notificationId);

    // then
    verify(notificationRepository).delete(notification);
  }

  @Test
  @DisplayName("알림 삭제 - 본인의 알림이 아니거나 없으면 NotificationNotFoundException 발생")
  void deleteNotification_NotFound_ThrowsException() {

    // given
    UUID notificationId = UUID.randomUUID();
    given(notificationRepository.findByIdAndUserId(notificationId, userId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> notificationService.deleteNotification(userId, notificationId))
        .isInstanceOf(NotificationNotFoundException.class);

    verify(notificationRepository, never()).delete(any());
  }

  @Test
  @DisplayName("알림 목록 조회 - limit보다 많은 데이터가 오면 hasNext가 true이고 하나를 잘라낸다.")
  void getNotifications_HasNextTrue() {

    // given
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 2, "DESCENDING", "createdAt");

    List<Notification> mockNotifications = new ArrayList<>();
    // limit(2) 보다 1개 더 많은 3개 생성
    for (int i = 0; i < 3; i ++) {
      Notification noti = Notification.builder().user(user).title("Title " + i).build();
      ReflectionTestUtils.setField(noti, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(noti, "createdAt", Instant.now().minusSeconds(i * 10)); // 과거순 정렬
      mockNotifications.add(noti);
    }

    given(notificationRepository.findNotificationsByCursor(
        eq(userId), eq(false), eq(null), eq(null), any(PageRequest.class)
    )).willReturn(mockNotifications);

    given(notificationRepository.countByUserId(userId)).willReturn(10L);
    given(notificationMapper.toDto(any())).willReturn(
        new NotificationDto(UUID.randomUUID(), "Title", "Content", NotificationLevel.INFO, userId, null)
    );

    // when
    CursorResponseNotificationDto response = notificationService.getNotifications(userId, request);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.data()).hasSize(request.limit()); // 2
    assertThat(response.totalCount()).isEqualTo(10L);
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("알림 목록 조회 - 두 번째 페이지(커서 존재)부터는 count 쿼리를 실행하지 않고 -1을 반환한다.")
  void getNotifications_WithCursor_DoesNotCount() {
    // given
    String cursor = Instant.now().toString(); // 커서 값 존재
    UUID idAfter = UUID.randomUUID();
    CursorPaginationRequest request = new CursorPaginationRequest(cursor, idAfter, 10, "DESCENDING", "createdAt");

    given(notificationRepository.findNotificationsByCursor(
        any(UUID.class), anyBoolean(), any(Instant.class), any(UUID.class), any(PageRequest.class)
    )).willReturn(new ArrayList<>());

    // when
    CursorResponseNotificationDto response = notificationService.getNotifications(userId, request);

    // then
    assertThat(response.totalCount()).isEqualTo(-1L);
    verify(notificationRepository, never()).countByUserId(any()); // 카운트 메서드 호출 안했는지 검증
  }

  @Test
  @DisplayName("알림 목록 조회 - limit 이하의 데이터가 오면 hasNext가 false이다.")
  void getNotifications_HasNextFalse() {

    // given
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 5, "DESCENDING", "createdAt");

    List<Notification> mockNotifications = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Notification noti = Notification.builder().user(user).title("Title " + i).build();
      ReflectionTestUtils.setField(noti, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(noti, "createdAt", Instant.now());
      mockNotifications.add(noti);
    }

    given(notificationRepository.findNotificationsByCursor(
        eq(userId), eq(false), eq(null), eq(null), any(PageRequest.class)
    )).willReturn(mockNotifications);

    given(notificationRepository.countByUserId(userId)).willReturn(2L);
    given(notificationMapper.toDto(any())).willReturn(
        new NotificationDto(UUID.randomUUID(), "Title", "Content", NotificationLevel.INFO, userId, null)
    );

    // when
    CursorResponseNotificationDto response = notificationService.getNotifications(userId, request);

    // then
    assertThat(response.hasNext()).isFalse();
    assertThat(response.data()).hasSize(2);
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("알림 목록 조회 - ASCENDING 정렬 시 파라미터에 true가 전달된다.")
  void getNotifications_Ascending_Success() {
    // given
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 10, "ASCENDING", "createdAt");

    given(notificationRepository.findNotificationsByCursor(eq(userId), eq(true), eq(null), eq(null), any(
        PageRequest.class))).willReturn(new ArrayList<>());
    given(notificationRepository.countByUserId(userId)).willReturn(0L);

    // when
    notificationService.getNotifications(userId, request);

    // then
    verify(notificationRepository).findNotificationsByCursor(eq(userId), eq(true), eq(null), eq(null), any(
        PageRequest.class));
  }

  @Test
  @DisplayName("알림 목록 조회 - 유효하지 않은 형식의 커서 전달 시 DateTimeParseException 발생")
  void getNotifications_InvalidCursor_ThrowsExceptions() {
    // given
    String invalidCursor = "2026-invalid-date-format";
    UUID idAfter = UUID.randomUUID();
    CursorPaginationRequest request = new CursorPaginationRequest(invalidCursor, idAfter, 10, "DESCENDING", "createdAt");

    // when & then
    assertThatThrownBy(() ->
        notificationService.getNotifications(userId, request)).isInstanceOf(InvalidCursorFormatException.class);
  }
}