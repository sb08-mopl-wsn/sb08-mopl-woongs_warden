package com.mopl.mopl.domain.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.request.CursorConversationRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.conversation.exception.ConversationAccessDeniedException;
import com.mopl.mopl.domain.conversation.exception.ConversationNotFoundException;
import com.mopl.mopl.domain.conversation.mapper.ConversationMapper;
import com.mopl.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.mopl.domain.dm.mapper.DirectMessageMapper;
import com.mopl.mopl.domain.dm.repository.DirectMessageRepository;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

  @InjectMocks
  private ConversationServiceImpl conversationService;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ConversationMapper conversationMapper;

  @Mock
  private DirectMessageRepository directMessageRepository;

  @Mock
  private DirectMessageMapper directMessageMapper;

  private User sender;
  private User receiver;
  private UUID currentUserId;
  private UUID withUserId;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.randomUUID();
    withUserId = UUID.randomUUID();

    sender = User.builder().email("sender@test.com").build();
    ReflectionTestUtils.setField(sender, "id", currentUserId);

    receiver = User.builder().email("receiver@test.com").build();
    ReflectionTestUtils.setField(receiver, "id", withUserId);
  }

  @Test
  @DisplayName("대화방 생성 - 기존 방이 없을 경우 새로 생성하여 반환한다.")
  void createConversation_Success_New() {

    // given
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    Conversation newConv = Conversation.builder().sender(sender).receiver(receiver).build();
    ConversationDto expectedDto = new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false, null);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(receiver));

    String pairKey = Conversation.buildPairKey(currentUserId, withUserId);
    given(conversationRepository.findByParticipantPairKey(pairKey)).willReturn(Optional.empty());
    given(conversationRepository.save(any(Conversation.class))).willReturn(newConv);
    given(directMessageRepository.findLatestMessage(any())).willReturn(Optional.empty());
    given(conversationMapper.toDto(newConv, currentUserId, null)).willReturn(expectedDto);

    // when
    ConversationDto result = conversationService.createConversation(currentUserId, request);

    // then
    assertThat(result).isNotNull();
    verify(conversationRepository).save(any(Conversation.class));
  }

  @Test
  @DisplayName("대화방 생성 - 이미 방이 존재할 경우(멱등성 보장) 새로 만들지 않고 기존 방을 반환한다.")
  void createConversation_Idempotent_ReturnsExisting() {

    // given
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    Conversation existingConv = Conversation.builder().sender(sender).receiver(receiver).build();
    ConversationDto expectedDto = new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false, null);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(receiver));
    // 기존 방이 존재한다고 설정
    String pairKey = Conversation.buildPairKey(currentUserId, withUserId);
    given(conversationRepository.findByParticipantPairKey(pairKey)).willReturn(Optional.of(existingConv));
    given(directMessageRepository.findLatestMessage(any())).willReturn(Optional.empty());
    given(conversationMapper.toDto(existingConv, currentUserId, null)).willReturn(expectedDto);

    // when
    ConversationDto result = conversationService.createConversation(currentUserId, request);

    // then
    assertThat(result).isNotNull();
    verify(conversationRepository, never()).save(any(Conversation.class));
  }

  @Test
  @DisplayName("대화방 생성 - 자기 자신과 방을 만들려고 하면 예외 발생")
  void createConversation_Self_ThrowsException() {

    // given
    ConversationCreateRequest request = new ConversationCreateRequest(currentUserId);

    // when & then
    assertThatThrownBy(() -> conversationService.createConversation(currentUserId, request))
        .isInstanceOf(ConversationAccessDeniedException.class);
  }

  @Test
  @DisplayName("상세 조회 - 내가 속하지 않은 대화방을 조회하려고 하면 권한 예외 발생")
  void getConversation_AccessDenied_ThrowsException() {

    // given
    UUID convId = UUID.randomUUID();
    UUID otherUserId1 = UUID.randomUUID();
    UUID otherUserId2 = UUID.randomUUID();

    // 다른 두 사람 방 생성
    User other1 = User.builder().build();
    ReflectionTestUtils.setField(other1, "id", otherUserId1);
    User other2 = User.builder().build();
    ReflectionTestUtils.setField(other2, "id", otherUserId2);
    Conversation otherConv = Conversation.builder().sender(other1).receiver(other2).build();

    given(conversationRepository.findById(convId)).willReturn(Optional.of(otherConv));

    // when & then
    // 제 3자 currentUserId가 조회 시도
    assertThatThrownBy(() -> conversationService.getConversation(currentUserId, convId))
        .isInstanceOf(ConversationAccessDeniedException.class);
  }

  @Test
  @DisplayName("목록 커서 페이징 - 정상적으로 페이징 처리가 되며 첫 페이지에만 count 쿼리가 실행된다.")
  void getMyConversations_FirstPage_ExecutesCount() {

    // given
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 10, "DESCENDING", "updatedAt");

    List<Conversation> mockConvs = new ArrayList<>();
    Conversation conv = Conversation.builder().sender(sender).receiver(receiver).build();
    ReflectionTestUtils.setField(conv, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(conv, "updatedAt", Instant.now());
    mockConvs.add(conv);

    given(conversationRepository.findMyConversationsByCursor(eq(currentUserId), eq(null), eq("updatedAt"), eq(false), eq(null), eq(null), any(
        PageRequest.class))).willReturn(mockConvs);
    given(conversationRepository.countMyConversationsByCursorCondition(currentUserId, null)).willReturn(5L);

    // when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result.totalCount()).isEqualTo(5L);
    verify(conversationRepository).countMyConversationsByCursorCondition(currentUserId, null);
  }

  @Test
  @DisplayName("목록 조회 - sortBy 파라미터가 updatedAt 또는 createdAt이 아닌 다른 값이면 400 에러 발생")
  void getMyConversations_InvalidSortBy_ThrowsException() {

    // given
    // 클라이언트가 지원하지 않는 createdAt이나 invalidSort를 보낸 상황
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 10, "DESCENDING", "invalidSort");

    // when & then
    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("지원합니다.");

    verify(conversationRepository, never()).findMyConversationsByCursor(any(), any(), any(), anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName("목록 조회 - 커서가 존재하는 두 번째 페이지부터는 count 쿼리를 실행하지 않고 -1을 반환한다.")
  void getMyConversations_WithCursor_DoesNotCount() {

    // given
    String cursor = Instant.now().toString();
    UUID idAfter = UUID.randomUUID();

    CursorConversationRequest request = new CursorConversationRequest(null, cursor, idAfter, 10, "DESCENDING", "updatedAt");

    given(conversationRepository.findMyConversationsByCursor(any(UUID.class), any(), any(), anyBoolean(), any(Instant.class), any(UUID.class), any(PageRequest.class)))
        .willReturn(new ArrayList<>());

    // when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result.totalCount()).isEqualTo(-1L);
    verify(conversationRepository, never()).countMyConversationsByCursorCondition(any(), any());
  }

  @Test
  @DisplayName("목록 조회 - limit(2)보다 많은 3개의 데이터가 반환되면, 1개를 잘라내고 hasNext=true, 정확한 nextCursor를 반환한다.")
  void getMyConversations_HasNextTrue_TruncatesAndSetsNextCursor() {
    // given
    int limit = 2;
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, limit, "DESCENDING", "updatedAt");

    List<Conversation> mockConvs = new ArrayList<>();
    // limit(2) 보다 1개 더 많은 3개 생성
    for (int i = 0; i < 3; i++) {
      Conversation conv = Conversation.builder().sender(sender).receiver(receiver).build();
      ReflectionTestUtils.setField(conv, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(conv, "updatedAt", Instant.now().minusSeconds(i * 10));
      mockConvs.add(conv);
    }

    // 마지막 데이터의 값 추출
    String expectedNextCursor = mockConvs.get(1).getUpdatedAt().toString();
    UUID expectedNextIdAfter = mockConvs.get(1).getId();

    given(conversationRepository.findMyConversationsByCursor(eq(currentUserId), eq(null), eq("updatedAt"), eq(false), eq(null), eq(null), any(PageRequest.class)))
        .willReturn(mockConvs);
    given(conversationRepository.countMyConversationsByCursorCondition(currentUserId, null)).willReturn(10L);
    given(directMessageRepository.findLatestMessage(any())).willReturn(Optional.empty());

    given(conversationMapper.toDto(any(), any(), any())).willReturn(
        new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false, null)
    );

    //when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.data()).hasSize(limit); // 3개가 2개로 정확히 잘렸는지 확인
    assertThat(result.totalCount()).isEqualTo(10L);
    assertThat(result.nextCursor()).isEqualTo(expectedNextCursor); // 마지막 데이터의 값과 일치하는지 확인
    assertThat(result.nextIdAfter()).isEqualTo(expectedNextIdAfter);
  }

  @Test
  @DisplayName("대화방 생성 - 동시에 생성 요청이 들어와서 유니크 제약 에러가 발생하면, 기존 방을 조회하여 반환한다 (TOCTOU 방어)")
  void createdConversation_RaceCondition_RecoversAndReturnsExisting() {

    // given
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    Conversation concurrentConv = Conversation.builder().sender(sender).receiver(receiver).build();
    ConversationDto expectedDto = new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false, null);
    String pairKey = Conversation.buildPairKey(currentUserId, withUserId);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(receiver));

    // 첫 조회 때는 방이 없다고 속임 (동시성 상황 가정)
    given(conversationRepository.findByParticipantPairKey(pairKey))
        .willReturn(Optional.empty()) // 첫번째 조회
        .willReturn(Optional.of(concurrentConv)); // catch 블록에서 두번째 조회

    // save 시 DB에서 유니크 에러 터짐
    given(conversationRepository.save(any(Conversation.class)))
        .willThrow(new DataIntegrityViolationException("Unique constraint violation"));

    given(directMessageRepository.findLatestMessage(any())).willReturn(Optional.empty());
    given(conversationMapper.toDto(concurrentConv, currentUserId, null)).willReturn(expectedDto);

    // when
    ConversationDto result = conversationService.createConversation(currentUserId, request);

    // then
    assertThat(result).isNotNull();
    verify(conversationRepository, times(2)).findByParticipantPairKey(pairKey);
  }

  @Test
  @DisplayName("목록 조회 - sortBy가 createdAt이면 정상 조회된다.")
  void getMyConversations_SortByCreatedAt_Success() {

    // given
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 10, "DESCENDING", "createdAt");
    given(conversationRepository.findMyConversationsByCursor(eq(currentUserId), eq(null), eq("createdAt"), eq(false), eq(null), eq(null), any(PageRequest.class)))
        .willReturn(new ArrayList<>());

    given(conversationRepository.countMyConversationsByCursorCondition(currentUserId, null)).willReturn(0L);

    // when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result).isNotNull();
    verify(conversationRepository).findMyConversationsByCursor(eq(currentUserId), eq(null), eq("createdAt"), eq(false), eq(null), eq(null), any(PageRequest.class));
    verify(conversationRepository).countMyConversationsByCursorCondition(currentUserId, null);
  }

  @Test
  @DisplayName("대화 목록 조회 - limit이 0 이하이면 예외 발생")
  void getMyConversations_InvalidLimit_ThrowsException() {

    // given
    CursorConversationRequest reqZero = new CursorConversationRequest(null, null, null, 0, "DESCENDING", "updatedAt");
    CursorConversationRequest reqMinus = new CursorConversationRequest(null, null, null, -1, "DESCENDING", "updatedAt");

    // when & then
    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, reqZero))
        .isInstanceOf(BusinessException.class).hasMessageContaining("limit은 1 이상의 값");
    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, reqMinus))
        .isInstanceOf(BusinessException.class).hasMessageContaining("limit은 1 이상의 값");
  }

  @Test
  @DisplayName("대화 목록 조회 - limit이 100 초과이면 예외 발생")
  void getMyConversations_LimitExceeds100_ThrowsException() {
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 101, "DESCENDING", "updatedAt");

    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, request))
        .isInstanceOf(BusinessException.class).hasMessageContaining("limit은 100 이하의 값");
  }

  @Test
  @DisplayName("대화 목록 조회 - 커서와 idAfter가 짝이 맞지 않으면 예외 발생")
  void getMyConversations_CursorAndIdMismatch_ThrowsException() {
    CursorConversationRequest req1 = new CursorConversationRequest(null, "2026-05-21T00:00:00Z", null, 10, "DESCENDING", "updatedAt");
    CursorConversationRequest req2 = new CursorConversationRequest(null, null, UUID.randomUUID(), 10, "DESCENDING", "updatedAt");

    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, req1))
        .isInstanceOf(BusinessException.class).hasMessageContaining("항상 함께 전달");
    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, req2))
        .isInstanceOf(BusinessException.class).hasMessageContaining("항상 함께 전달");
  }

  @Test
  @DisplayName("대화 목록 조회 - 정렬 방향(sortDirection)이 잘못되면 예외 발생")
  void getMyConversations_InvalidSortDirection_ThrowsException() {
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 10, "wrongDirection", "updatedAt");

    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, request))
        .isInstanceOf(BusinessException.class).hasMessageContaining("정렬 방향(sortDirection)");
  }

  @Test
  @DisplayName("대화 목록 조회 - 커서 시간 파싱 실패 시 예외 발생")
  void getMyConversations_InvalidCursorFormat_ThrowsException() {
    CursorConversationRequest request = new CursorConversationRequest(null, "wrong-format", UUID.randomUUID(), 10, "DESCENDING", "updatedAt");

    assertThatThrownBy(() -> conversationService.getMyConversations(currentUserId, request))
        .isInstanceOf(BusinessException.class).hasMessageContaining("잘못된 형식의 커서 데이터");
  }

  @Test
  @DisplayName("상대방과의 대화 조회 - 대화방이 존재하지 않으면 예외 발생")
  void getConversationWith_NotFound_ThrowsException() {

    // given
    UUID withUserId = UUID.randomUUID();
    String pairKey = Conversation.buildPairKey(currentUserId, withUserId);

    given(conversationRepository.findByParticipantPairKey(pairKey)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> conversationService.getConversationWith(currentUserId, withUserId))
        .isInstanceOf(ConversationNotFoundException.class);
  }

  @Test
  @DisplayName("대화 목록 조회 - sortDirection이 ASCENDING이면 정상 조회된다")
  void getMyConversations_AscendingSort_Success() {
    // given
    CursorConversationRequest request = new CursorConversationRequest(null, null, null, 10, "ASCENDING", "updatedAt");
    given(conversationRepository.findMyConversationsByCursor(eq(currentUserId), eq(null), eq("updatedAt"), eq(true), any(), any(), any()))
        .willReturn(new ArrayList<>());
    given(conversationRepository.countMyConversationsByCursorCondition(currentUserId, null)).willReturn(0L);

    // when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result).isNotNull();
    // ASCENDING일 때 isAscending = true로 호출되는지 검증
    verify(conversationRepository).findMyConversationsByCursor(eq(currentUserId), eq(null), eq("updatedAt"), eq(true), any(), any(), any());
  }

  @Test
  @DisplayName("상대방과의 대화 조회 - 대화방이 존재하면 정상 조회된다")
  void getConversationWith_Success() {
    // given
    UUID withUserId = UUID.randomUUID();
    String pairKey = Conversation.buildPairKey(currentUserId, withUserId);

    Conversation conversation = Conversation.builder()
        .sender(sender)
        .receiver(receiver)
        .build();
    ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());

    ConversationDto expectedDto = new ConversationDto(
        conversation.getId(),
        new UserSummary(withUserId, null, null),
        null,
        false,
        null
    );

    given(conversationRepository.findByParticipantPairKey(pairKey))
        .willReturn(Optional.of(conversation));
    given(directMessageRepository.findLatestMessage(any()))
        .willReturn(Optional.empty());
    given(conversationMapper.toDto(conversation, currentUserId, null))
        .willReturn(expectedDto);

    // when
    ConversationDto result = conversationService.getConversationWith(currentUserId, withUserId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(conversation.getId());
    verify(conversationRepository).findByParticipantPairKey(pairKey);
  }
}
