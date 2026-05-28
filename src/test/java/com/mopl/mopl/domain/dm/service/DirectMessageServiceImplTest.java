package com.mopl.mopl.domain.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.DirectMessageReadEvent;
import com.mopl.mopl.global.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
  private ApplicationEventPublisher eventPublisher;

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

    ArgumentCaptor<DirectMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(DirectMessageCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    DirectMessageCreatedEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.receiverId()).isEqualTo(receiverUserId);
    assertThat(publishedEvent.conversationId()).isEqualTo(conversationId);
    assertThat(publishedEvent.messageDto().content()).isEqualTo("안녕하세요~~");
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
    verify(eventPublisher, never()).publishEvent(any());
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
    UUID idAfter = UUID.randomUUID();
    CursorPaginationRequest request = new CursorPaginationRequest(cursor, idAfter, 10, "DESCENDING", "createdAt");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findMessagesByCursor(any(), any(), any(), any(Integer.class), any()))
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

  @Test
  @DisplayName("메시지 목록 조회 - limit이 0 이하일 경우 400 예외 발생")
  void getMessages_InvalidLimit_ThrowsException() {

    //given
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 0, "DESCENDING", "createdAt");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    // when & then
    assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("limit은 1 이상의 값");
  }

  @Test
  @DisplayName("메시지 목록 조회 - cursor와 idAfter 중 하나만 전달될 경우 400 예외 발생")
  void getMessages_MismatchedCursor_ThrowsException() {

    // given
    // cursor는 있는데 idAfter는 없는 요청
    CursorPaginationRequest request = new CursorPaginationRequest(Instant.now().toString(), null, 10, "DESCENDING", "createdAt");

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    // when & then
    assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("항상 함께 전달되어야 합니다.");
  }
  
  @Test
  @DisplayName("DM 목록 조회 - 정렬 방향(sortDirection)이 잘못되면 예외 발생")
  void getMessages_InvalidSortDirection_ThrowsException() {

    // given
    Conversation conversation = mock(Conversation.class);
    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(conversation.getSender()).willReturn(mockUser);

    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 10,"wrongDirection", "createdAt");

    // when & then
    assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
        .isInstanceOf(BusinessException.class).hasMessageContaining("정렬 방향");
  }

  @Test
  @DisplayName("DM 목록 조회 - 커서 시간 파싱 실패(DateTimeParseException) 시 예외 발생")
  void getMessages_InvalidCursorFormat_ThrowsException() {

    // given
    Conversation conversation = mock(Conversation.class);
    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(conversation.getSender()).willReturn(mockUser);

    CursorPaginationRequest request = new CursorPaginationRequest("wrong-date-foramt", UUID.randomUUID(), 10, "DESCENDING", "createdAt");

    // when & then
    assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("잘못된 형식의 커서 데이터");
  }

  @Test
  @DisplayName("메시지 읽음 처리 - 성공 시 Watermark와 읽음 상태를 업데이트하고 이벤트를 발행한다.")
  void readMessage_Success() {

    // given
    UUID messageId = UUID.randomUUID();
    Conversation conversation = mock(Conversation.class);
    DirectMessage message = mock(DirectMessage.class);

    given(conversation.getId()).willReturn(conversationId);
    given(message.getConversation()).willReturn(conversation);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
    given(message.getCreatedAt()).willReturn(Instant.now());

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(conversation.getSender()).willReturn(mockUser);
    given(conversation.updateLastReadAt(any(), any())).willReturn(true);

    // when
    directMessageService.readMessage(currentUserId, conversationId, messageId);

    // then
    verify(conversation).updateLastReadAt(eq(currentUserId), any(Instant.class));
    verify(conversation).updateUnreadStatus(false);
    verify(eventPublisher).publishEvent(any(DirectMessageReadEvent.class));
  }

  @Test
  @DisplayName("메시지 읽음 처리 - Watermark 갱신이 발생하지 않으면 브로드캐스팅 이벤트를 발행하지 않는다.")
  void readMessage_WatermarkNotUpdated_DoesNotPublishEvent() {

    // given
    UUID messageId = UUID.randomUUID();
    Conversation conversation = mock(Conversation.class);
    DirectMessage message = mock(DirectMessage.class);

    given(conversation.getId()).willReturn(conversationId);
    given(message.getConversation()).willReturn(conversation);

    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
    given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
    given(message.getCreatedAt()).willReturn(Instant.now());

    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(conversation.getSender()).willReturn(mockUser);

    // 워터마크 갱신 실패
    given(conversation.updateLastReadAt(any(), any())).willReturn(false);

    // when
    directMessageService.readMessage(currentUserId, conversationId, messageId);

    // then
    verify(conversation).updateLastReadAt(eq(currentUserId), any(Instant.class));
    verify(conversation, never()).updateUnreadStatus(anyBoolean());

    verify(eventPublisher, never()).publishEvent(any(DirectMessageReadEvent.class));
  }

  @Test
  @DisplayName("메시지 읽음 처리 - 대화방 참여자가 아닌 제 3자가 호출하면 권한 예외 발생")
  void readMessage_AccessDenied_ThrowsException() {

    // given
    UUID messageId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();

    Conversation conversation = mock(Conversation.class);
    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    User sender = mock(User.class);
    given(sender.getId()).willReturn(UUID.randomUUID());
    User receiver = mock(User.class);
    given(receiver.getId()).willReturn(UUID.randomUUID());

    given(conversation.getSender()).willReturn(sender);
    given(conversation.getReceiver()).willReturn(receiver);

    // when & then
    assertThatThrownBy(() -> directMessageService.readMessage(outsiderId, conversationId, messageId))
        .isInstanceOf(ConversationAccessDeniedException.class);
  }

  @Test
  @DisplayName("메시지 읽음 처리 - DB에 존재하지 않는 메시지 ID를 요청하면 예외가 발생한다.")
  void readMessage_MessageNotFound_ThrowsException() {

    // given
    UUID messageId = UUID.randomUUID();
    Conversation conversation = mock(Conversation.class);

    // 대화방 찾기
    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

    // 권한 검사 통과
    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(conversation.getSender()).willReturn(mockUser);

    // 메시지 리포지토리에서 메시지 못찾음
    given(messageRepository.findById(messageId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> directMessageService.readMessage(currentUserId, conversationId, messageId))
        .isInstanceOf(BusinessException.class).hasMessageContaining("찾을 수 없습니다.");
  }

  @Test
  @DisplayName("메시지 읽음 처리 - 해커가 다른 대화방의 메시지 ID를 조작해 요청하면 보안 예외가 발생한다 (IDOR 방어).")
  void readMessage_MessageBelongsToOtherConversation_ThrowsException() {

    // given
    UUID messageId = UUID.randomUUID();
    Conversation myConversation = mock(Conversation.class);
    Conversation otherConversation = mock(Conversation.class);
    DirectMessage hackedMessage = mock(DirectMessage.class);

    // 내 대화방 찾기 및 권한 검사 정상 통과
    given(conversationRepository.findById(conversationId)).willReturn(Optional.of(myConversation));
    User mockUser = mock(User.class);
    given(mockUser.getId()).willReturn(currentUserId);
    given(myConversation.getSender()).willReturn(mockUser);

    // 파라미터로 넘어온 메시지가 다른 방 소속
    given(messageRepository.findById(messageId)).willReturn(Optional.of(hackedMessage));
    given(hackedMessage.getConversation()).willReturn(otherConversation);
    given(otherConversation.getId()).willReturn(UUID.randomUUID());

    // when & then
    assertThatThrownBy(() -> directMessageService.readMessage(currentUserId, conversationId, messageId))
        .isInstanceOf(BusinessException.class).hasMessageContaining("해당 대화방의 메시지가 아닙니다");
  }
}