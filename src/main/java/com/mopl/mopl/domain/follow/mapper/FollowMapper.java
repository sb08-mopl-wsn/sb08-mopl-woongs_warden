package com.mopl.mopl.domain.follow.mapper;

import com.mopl.mopl.domain.follow.dto.FollowDto;
import com.mopl.mopl.domain.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowMapper {

  @Mapping(source = "follower.id", target = "followerId")
  @Mapping(source = "followee.id", target = "followeeId")
  FollowDto toDto(Follow follow);
}
