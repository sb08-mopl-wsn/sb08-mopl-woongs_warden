package com.mopl.mopl.global.event;

import java.util.UUID;

public record FollowEvent(
    UUID followerId,      // 팔로우를 건 사람 (알림 유발자)
    String followerName,  // 알림 메시지 생성을 위한 팔로워 이름
    UUID followeeId       // 팔로우를 당한 사람 (알림 수신자)
) {

}