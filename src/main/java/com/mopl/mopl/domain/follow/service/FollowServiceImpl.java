package com.mopl.mopl.domain.follow.service;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import com.mopl.mopl.domain.follow.entity.Follow;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final FollowMapper followMapper;

  /**
   * 특정 유저를 팔로우하는 메서드
   * 이미 팔로우 중인 경우 예외를 발생시키지 않고 기존 팔로우 정보 반환
   * @param followerId 팔로우를 요청하는 주체(현재 로그인한 유저)의 ID
   * @param request 팔로우할 대상(followee)의 ID가 담긴 요청 객체
   * @return 생성되거나 이미 존재하는 팔로우 정보 DTO
   */
  @Override
  @Transactional
  public FollowDto follow(UUID followerId, FollowRequest request) {

    // 본인 팔로우인지 체크
    if (followerId.equals(request.followeeId())) {
      throw new SelfFollowException();
    }

    // TODO: 추후 유저 관련 exception이 추가되면, 삭제 후 USER쪽에서 만든 exception으로 교체 필요
    User follower = userRepository.findById(followerId)
        .orElseThrow(FollowerNotFoundException::new);
    User followee = userRepository.findById(request.followeeId())
        .orElseThrow(FolloweeNotFoundException::new);

    // 이미 팔로우 중일 땐, 기존 정보 반환
    Optional<Follow> existingFollow = followRepository.findByFollowerAndFollowee(follower, followee);
    if (existingFollow.isPresent()) {
      return followMapper.toDto(existingFollow.get());
    }

    Follow follow = Follow.builder()
        .follower(follower)
        .followee(followee)
        .build();

    try {
      Follow savedFollow = followRepository.save(follow);
      // TODO: 추후 알림 Event 발행 시 이 곳에 작성
      return followMapper.toDto(savedFollow);
    } catch (DataIntegrityViolationException e) {
      /*
      동시성 이슈로 두 스레드가 동시에 save 시도 시, 유니크 제약 조건에 걸려서 예외가 발생하면
      이미 저장된 것으로 간주하고 DB에서 다시 조회해서 반환
      * */
      Follow concurrentFollow = followRepository.findByFollowerAndFollowee(follower, followee)
          .orElseThrow(FollowNotFoundException::new);

      return followMapper.toDto(concurrentFollow);
    }
  }

  /**
   * 특정 팔로우 관계를 취소(삭제)하는 메서드
   * 다른 유저의 팔로우 내역 임의 삭제 방지를 위해서 요청자 ID와 일치하는지 검증
   * @param followerId 언팔로우를 요청하는 주체(현재 로그인한 유저)의 ID
   * @param followId 취소할 팔로우 관계의 고유 ID
   */
  @Override
  @Transactional
  public void unfollow(UUID followerId, UUID followId) {

    Follow follow = followRepository.findByIdAndFollowerId(followId, followerId)
        .orElseThrow(() -> new FollowNotFoundException(followId));

    followRepository.delete(follow);
  }

  /**
   * 현재 요청자가 특정 유저를 팔로우하고 있는지 확인하는 메서드
   * @param followerId 상태를 확인할 주체(현재 로그인한 유저)의 ID
   * @param followeeId 팔로우 여부를 확인할 대상 유저의 ID
   * @return 팔로우 중이면 true, 아니면 false 반환
   */
  @Override
  public boolean isFollowedByMe(UUID followerId, UUID followeeId) {
    // 로그인 안한 사용자의 조회 요청 방어
    if (followerId == null) return false;

    return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
  }

  /**
   * 특정 유저를 팔로우하고 있는 팔로워(구독자)의 총 숫자를 조회하는 메서드
   * @param followeeId 팔로워 수를 조회할 대상 유저의 ID
   * @return 해당 유저를 팔로우하는 유저의 총 수
   */
  @Override
  public long getFollowerCount(UUID followeeId) {

    return followRepository.countByFolloweeId(followeeId);
  }
}
