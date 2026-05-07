package com.mopl.mopl.domain.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.domain.follow.entity.Follow;
import com.mopl.mopl.domain.follow.exception.FollowException;
import com.mopl.mopl.domain.follow.exception.FollowNotFoundException;
import com.mopl.mopl.domain.follow.exception.FolloweeNotFoundException;
import com.mopl.mopl.domain.follow.exception.FollowerNotFoundException;
import com.mopl.mopl.domain.follow.exception.SelfFollowException;
import com.mopl.mopl.domain.follow.mapper.FollowMapper;
import com.mopl.mopl.domain.follow.repository.FollowRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
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
class FollowServiceImplTest {

  @InjectMocks
  private FollowServiceImpl followService;

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FollowMapper followMapper;

  private User follower;
  private User followee;
  private UUID followerId;
  private UUID followeeId;

  @BeforeEach
  void setUp() {
    followerId = UUID.randomUUID();
    followeeId = UUID.randomUUID();

    follower = User.builder().email("follower@test.com").build();
    ReflectionTestUtils.setField(follower, "id", followerId);

    followee = User.builder().email("followee@test.com").build();
    ReflectionTestUtils.setField(followee, "id", followeeId);
  }

  @Test
  @DisplayName("팔로우 성공")
  void follow_Success() {
    // given
    FollowRequest request = new FollowRequest(followeeId);
    Follow follow = Follow.builder().follower(follower).followee(followee).build();
    FollowDto expectedDto = new FollowDto(UUID.randomUUID(), followeeId, followerId);

    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
    given(followRepository.findByFollowerAndFollowee(follower, followee)).willReturn(Optional.empty()); // 기존 팔로우 X
    given(followRepository.save(any(Follow.class))).willReturn(follow);
    given(followMapper.toDto(follow)).willReturn(expectedDto);

    // when
    FollowDto result = followService.follow(followerId, request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.followerId()).isEqualByComparingTo(followerId);
    verify(followRepository).save(any(Follow.class));
  }

  @Test
  @DisplayName("팔로우 - 자기 자신 팔로우 시 예외 발생")
  void follow_SelfFollow_ThrowsException() {
    // given
    FollowRequest request = new FollowRequest(followerId);

    // when & then
    assertThatThrownBy(() -> followService.follow(followerId, request))
        .isInstanceOf(SelfFollowException.class);

    verify(userRepository, never()).findById(any());
    verify(followRepository, never()).save(any());
  }

  @Test
  @DisplayName("팔로우 - 이미 팔로우 중이면 새로 저장하지 않고 기존 정보를 반환한다.")
  void follow_AlreadyFollowing_ReturnsExisting() {
    // given
    FollowRequest request = new FollowRequest(followeeId);
    Follow existingFollow = Follow.builder().follower(follower).followee(followee).build();
    FollowDto expectedDto = new FollowDto(UUID.randomUUID(), followeeId, followerId);

    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));

    // 기존 팔로우가 존재한다고 가정
    given(followRepository.findByFollowerAndFollowee(follower, followee)).willReturn(Optional.of(existingFollow));
    given(followMapper.toDto(existingFollow)).willReturn(expectedDto);

    // when
    FollowDto result = followService.follow(followerId, request);

    // then
    assertThat(result).isNotNull();
    verify(followRepository, never()).save(any(Follow.class));
  }

  @Test
  @DisplayName("언팔로우 - 정상 삭제 처리")
  void unfollow_Success() {
    // given
    UUID followId = UUID.randomUUID();
    Follow follow = Follow.builder().follower(follower).followee(followee).build();

    given(followRepository.findByIdAndFollowerId(followId, followerId)).willReturn(Optional.of(follow));

    // when
    followService.unfollow(followerId, followId);

    // then
    verify(followRepository).delete(follow);
  }

  @Test
  @DisplayName("언팔로우 - 팔로우 내역을 찾을 수 없으면 예외 발생")
  void unfollow_NotFound_ThrowsException() {
    // given
    UUID followId = UUID.randomUUID();
    given(followRepository.findByIdAndFollowerId(followId, followerId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.unfollow(followerId, followId))
        .isInstanceOf(FollowNotFoundException.class);

    verify(followRepository, never()).delete(any());
  }

  @Test
  @DisplayName("팔로우 여부 확인 - 본인 ID가 null이면 false 반환 (비로그인 상태 방어)")
  void isFollowedByMe_ReturnsFalse_WhenFollowerIdIsNull() {
    // when
    boolean isFollowed = followService.isFollowedByMe(null, followeeId);

    // then
    assertThat(isFollowed).isFalse();
    verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(), any());
  }

  @Test
  @DisplayName("팔로우 여부 확인 - 팔로우 중이면 true, 아니면 false 반환 (Repository 분기)")
  void isFollowedByMe_ReturnsRepositoryResult() {
    // given
    given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).willReturn(true);

    // when
    boolean isFollowed = followService.isFollowedByMe(followerId, followeeId);

    // then
    assertThat(isFollowed).isTrue();
    verify(followRepository).existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  @Test
  @DisplayName("팔로워 수 조회 - Repository 로직 위임 검증")
  void getFollowerCount_DelegatesToRepository() {
    // given
    long expectedCount = 5L;
    given(followRepository.countByFolloweeId(followeeId)).willReturn(expectedCount);

    // when
    long count = followService.getFollowerCount(followeeId);

    // then
    assertThat(count).isEqualTo(expectedCount);
    verify(followRepository).countByFolloweeId(followeeId);
  }

  @Test
  @DisplayName("팔로우 요청 - 요청한 유저(follower)를 찾을 수 없으면 FOLLOWER_NOT_FOUND 예외 발생")
  void follow_FollowerNotFound_ThrowsException() {
    // given
    FollowRequest request = new FollowRequest(followeeId);
    given(userRepository.findById(followerId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.follow(followerId, request))
        .isInstanceOf(FollowerNotFoundException.class);

    verify(followRepository, never()).save(any());
  }

  @Test
  @DisplayName("팔로우 요청 - 대상 유저(followee)를 찾을 수 없으면 FOLLOWEE_NOT_FOUND 예외 발생")
  void follow_FolloweeNotFound_ThrowsException() {
    // given
    FollowRequest request = new FollowRequest(followeeId);
    given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
    given(userRepository.findById(followeeId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.follow(followerId, request))
        .isInstanceOf(FolloweeNotFoundException.class);

    verify(followRepository, never()).save(any());
  }
}