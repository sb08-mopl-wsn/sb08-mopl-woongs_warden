package com.mopl.mopl.domain.follow.service;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.dto.FollowRequest;
import java.util.UUID;

public interface FollowService {

  FollowDto follow(UUID followerId, FollowRequest request);
  void unfollow(UUID followerId, UUID followId);
  FollowDto isFollowedByMe(UUID followerId, UUID followeeId);
  long getFollowerCount(UUID followeeId);
}
