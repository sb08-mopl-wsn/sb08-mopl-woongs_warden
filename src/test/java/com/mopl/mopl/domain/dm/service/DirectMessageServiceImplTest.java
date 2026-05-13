package com.mopl.mopl.domain.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.conversation.exception.ConversationAccessDeniedException;
import com.mopl.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.mopl.domain.dm.dto.CursorResponseDirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.dto.DirectMessageSendRequest;
import com.mopl.mopl.domain.dm.entity.DirectMessage;
import com.mopl.mopl.domain.dm.mapper.DirectMessageMapper;
import com.mopl.mopl.domain.dm.repository.DirectMessageRepository;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.sse.service.SseService;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceImplTest {

  @InjectMocks
  private DirectMessageServiceImpl directMessageService;

  @Mock
  private DirectMessageRepository messageRepository;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private DirectMessageMapper messageMapper;

  @Mock
  private SseService sseService;

  private User sender;
  private User receiver;
  private Conversation conversation;
  private UUID currentUserId;
  private UUID receiverUserId;
  private UUID conversationId;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.randomUUID();
    receiverUserId = UUID.randomUUID();
    conversationId = UUID.randomUUID();

    sender = User.builder().email("sender@test.com").build();
    ReflectionTestUtils.setField(sender, "id", currentUserId);

    receiver = User.builder().email("receiver@test.com").build();
    ReflectionTestUtils.setField(receiver, "id", receiverUserId);

    conversation = Conversation.builder().sender(sender).receiver(receiver).build();
    ReflectionTestUtils.setField(conversation, "id", conversationId);
  }

  @Test
  @DisplayName("메시지 전송 - 정상적으로 저장되고, 방 상태가 갱신되며, 상대방에게 SSE 푸시가 발송된다.")
  void sendMessage_Success() {

    // given
    DirectMessageSendRequest request = new DirectMessageSendRequest("안녕하세요~~");
    DirectMessage savedMessage = DirectMessage.builder()
        .conversation(conversation)
        .sender(sender)
        .content(request.content())
        .build();
    DirectMessageDto expectedDto = new DirectMessageDto(UUID.randomUUID(), conversationId, request.content(),
        new UserSummary(currentUserId, null, null), new UserSummary(receiverUserId, null, null),
        Instant.now());

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(messageRepository.save(any(DirectMessage.class))).willReturn(savedMessage);
    given(messageMapper.toDto(savedMessage)).willReturn(expectedDto);

    // when
    DirectMessageDto result = directMessageService.sendMessage(currentUserId, conversationId, request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("안녕하세요~~");
    assertThat(conversation.isHasUnread()).isTrue();

    verify(messageRepository).save(any(DirectMessage.class));
    verify(sseService).sendNotification(eq(receiverUserId), any(String.class));
  }

  @Test
  @DisplayName("메시지 전송 - 내가 속하지 않은 대화방에 메시지를 보내려 하면 권한 예외가 발생한다.")
  void sendMessage_AccessDenied_ThrowsException() {

    // given
    UUID thirdPartyId = UUID.randomUUID(); // 제 3자
    DirectMessageSendRequest request = new DirectMessageSendRequest("해킹 시도");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    // when & then
    assertThatThrownBy(() -> directMessageService.sendMessage(thirdPartyId, conversationId, request))
        .isInstanceOf(ConversationAccessDeniedException.class);

    verify(messageRepository, never()).save(any(DirectMessage.class));
    verify(sseService, never()).sendNotification(any(), any());
  }

  @Test
  @DisplayName("메시지 목록 조회 - 첫 페이지 조회 시 전체 카운트 쿼리를 실행하며 정상적으로 페이징된다.")
  void getMessages_FirstPage_ExecutesCountAndPaginates() {

    // given
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 10, "DESCENDING", "createdAt");

    List<DirectMessage> mockMessages = new ArrayList<>();
    DirectMessage msg = DirectMessage.builder().conversation(conversation).sender(sender).content("test").build();
    ReflectionTestUtils.setField(msg, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(msg, "createdAt", Instant.now());
    mockMessages.add(msg);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findMessagesByCursor(eq(conversationId), eq(null), eq(null), eq(10), eq("DESCENDING")))
        .willReturn(mockMessages);
    given(messageRepository.countByConversationId(conversationId)).willReturn(100L);

    // when
    CursorResponseDirectMessageDto result = directMessageService.getMessages(currentUserId, conversationId, request);

    // then
    assertThat(result.totalCount()).isEqualTo(100L);
    verify(messageRepository).countByConversationId(conversationId);
  }

  @Test
  @DisplayName("메시지 목록 조회 - sortBy 파라미터가 createdAt이 아닌 다른 값이면 400 예외 발생")
  void getMessages_InvalidSortBy_ThrowsException() {

    // given
    // 지원하지 않는 정렬 기준이나 잘못된 값 보낸 상황
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 10, "DESCENDING", "updatedAt");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    // when & then
    assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("'createdAt'만 지원합니다.");

    verify(messageRepository, never()).findMessagesByCursor(any(), any(), any(), any(Integer.class), any());
  }

  @Test
  @DisplayName("메시지 목록 조회 - 커서가 존재하는 두 번째 페이지부터는 count 쿼리를 실행하지 않고 -1을 반환한다.")
  void getMessages_WithCursor_DoesNotCount() {

    // given
    String cursor = Instant.now().toString();
    CursorPaginationRequest request = new CursorPaginationRequest(cursor, null, 10, "DESCENDING", "createdAt");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findMessagesByCursor(eq(conversationId), any(Instant.class), eq(null), eq(10), eq("DESCENDING")))
        .willReturn(new ArrayList<>()); // 빈 리스트 반환

    // when
    CursorResponseDirectMessageDto result = directMessageService.getMessages(currentUserId, conversationId, request);

    // then
    assertThat(result.totalCount()).isEqualTo(-1L);
    verify(messageRepository, never()).countByConversationId(any());
  }

  @Test
  @DisplayName("메시지 목록 조회 - limit(2)보다 많은 3개의 데이터가 반환되면, 1개를 잘라내고 hasNext=true, 정확한 nextCursor를 반환한다.")
  void getMessages_HasNextTrue_TruncatesAndSetNextCursor() {

    // given
    int limit = 2;
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, limit, "DESCENDING", "createdAt");

    List<DirectMessage> mockMessages = new ArrayList<>();
    // limit(2)보다 1개 더 많은 3개 생성
    for (int i = 0; i < 3; i++ ) {
      DirectMessage msg = DirectMessage.builder().conversation(conversation).sender(sender).content("test" + i).build();
      ReflectionTestUtils.setField(msg, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(msg, "createdAt", Instant.now().minusSeconds(i * 10));
      mockMessages.add(msg);
    }

    // 마지막 데이터 값 추출
    String expectedNextCursor = mockMessages.get(1).getCreatedAt().toString();
    UUID expectedNextIdAfter = mockMessages.get(1).getId();

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findMessagesByCursor(eq(conversationId), eq(null), eq(null), eq(limit), eq("DESCENDING")))
        .willReturn(mockMessages);
    given(messageRepository.countByConversationId(conversationId)).willReturn(10L);

    // 매퍼가 호출될 때 가짜 DTO 반환하도록 설정
    given(messageMapper.toDto(any())).willReturn(
        new DirectMessageDto(UUID.randomUUID(), conversationId, "test", null, null, Instant.now())
    );

    // when
    CursorResponseDirectMessageDto result = directMessageService.getMessages(currentUserId, conversationId, request);

    // then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.data()).hasSize(limit);
    assertThat(result.totalCount()).isEqualTo(10L);
    assertThat(result.nextCursor()).isEqualTo(expectedNextCursor);
    assertThat(result.nextIdAfter()).isEqualTo(expectedNextIdAfter);
  }
}