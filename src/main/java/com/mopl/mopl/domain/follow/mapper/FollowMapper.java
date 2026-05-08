package com.mopl.mopl.domain.follow.mapper;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {

  @Mapping(source = "follower.id", target = "followerId")
  @Mapping(source = "followee.id", target = "followeeId")
  FollowDto toDto(Follow follow);
}
