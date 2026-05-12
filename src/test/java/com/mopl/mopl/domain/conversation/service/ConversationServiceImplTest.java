package com.mopl.mopl.domain.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.conversation.dto.ConversationCreateRequest;
import com.mopl.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.mopl.domain.conversation.dto.response.CursorResponseConversationDto;
import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.domain.conversation.exception.ConversationAccessDeniedException;
import com.mopl.mopl.domain.conversation.mapper.ConversationMapper;
import com.mopl.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.dto.CursorPaginationRequest;
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
class ConversationServiceImplTest {

  @InjectMocks
  private ConversationServiceImpl conversationService;

  @Mock
  private ConversationRepository conversationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ConversationMapper conversationMapper;

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
    ConversationDto expectedDto = new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(receiver));
    given(conversationRepository.findConversationBetweenUsers(currentUserId, withUserId)).willReturn(Optional.empty());
    given(conversationRepository.save(any(Conversation.class))).willReturn(newConv);
    given(conversationMapper.toDto(newConv, currentUserId)).willReturn(expectedDto);

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
    ConversationDto expectedDto = new ConversationDto(UUID.randomUUID(), new UserSummary(withUserId, null, null), null, false);

    given(userRepository.findById(currentUserId)).willReturn(Optional.of(sender));
    given(userRepository.findById(withUserId)).willReturn(Optional.of(receiver));
    // 기존 방이 존재한다고 설정
    given(conversationRepository.findConversationBetweenUsers(currentUserId, withUserId)).willReturn(Optional.of(existingConv));
    given(conversationMapper.toDto(existingConv, currentUserId)).willReturn(expectedDto);

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
    CursorPaginationRequest request = new CursorPaginationRequest(null, null, 10, "DESCENDING", "updatedAt");

    List<Conversation> mockConvs = new ArrayList<>();
    Conversation conv = Conversation.builder().sender(sender).receiver(receiver).build();
    ReflectionTestUtils.setField(conv, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(conv, "updatedAt", Instant.now());
    mockConvs.add(conv);

    given(conversationRepository.findMyConversationsByCursorDesc(eq(currentUserId), eq(null), eq(null), any(
        PageRequest.class))).willReturn(mockConvs);
    given(conversationRepository.countMyConversations(currentUserId)).willReturn(5L);

    // when
    CursorResponseConversationDto result = conversationService.getMyConversations(currentUserId, request);

    // then
    assertThat(result.totalCount()).isEqualTo(5L);
    verify(conversationRepository).countMyConversations(currentUserId);
  }
}