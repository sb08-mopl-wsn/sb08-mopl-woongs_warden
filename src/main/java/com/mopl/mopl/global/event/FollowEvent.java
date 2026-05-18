package com.mopl.mopl.global.event;

import java.util.UUID;

/**
 * 팔로우 이벤트
 * @param followerId 팔로우를 건 사람 (알림 유발자)
 * @param followerName 알림 메시지 생성을 위한 팔로워 이름
 * @param followeeId 팔로우를 당한 사람 (알림 수신자)
 */
public record FollowEvent(
    UUID followerId,
    String followerName,
    UUID followeeId
) {

  // 팩토리 메서드
  public static FollowEvent of(UUID followerId, String followerName, UUID followeeId) {
    return new FollowEvent(followerId, followerName, followeeId);
  }
}